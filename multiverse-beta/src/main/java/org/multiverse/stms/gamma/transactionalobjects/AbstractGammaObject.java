package org.multiverse.stms.gamma.transactionalobjects;

import org.multiverse.api.Lock;
import org.multiverse.api.LockMode;
import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.Listeners;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.utils.ToolUnsafe;
import sun.misc.Unsafe;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

@SuppressWarnings({"OverlyComplexClass"})
public abstract class AbstractGammaObject implements GammaObject, Lock {

    //it is important that the maximum threshold is not larger than 1023 (there are 10 bits for the readonly count)
    private static final int READBIASED_THRESHOLD = 128;

    public static final long MASK_OREC_EXCLUSIVELOCK = 0x8000000000000000L;
    public static final long MASK_OREC_UPDATELOCK = 0x4000000000000000L;
    public static final long MASK_OREC_READBIASED = 0x2000000000000000L;
    public static final long MASK_OREC_READLOCKS = 0x1FFFFF0000000000L;
    public static final long MASK_OREC_SURPLUS = 0x000000FFFFFFFE00L;
    public static final long MASK_OREC_READONLY_COUNT = 0x00000000000003FFL;

    protected static final Unsafe ___unsafe = ToolUnsafe.getUnsafe();
    protected static final long listenersOffset;
    protected static final long valueOffset;

    static {
        try {
            listenersOffset = ___unsafe.objectFieldOffset(
                    AbstractGammaObject.class.getDeclaredField("listeners"));
            valueOffset = ___unsafe.objectFieldOffset(
                    AbstractGammaObject.class.getDeclaredField("orec"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public final GammaStm stm;

    @SuppressWarnings({"UnusedDeclaration"})
    public volatile Listeners listeners;

    @SuppressWarnings({"VolatileLongOrDoubleField"})
    public volatile long version;

    @SuppressWarnings({"VolatileLongOrDoubleField"})
    public volatile long orec;

    //This field has a controlled JMM problem (just like the hashcode of String).
    protected int identityHashCode;

    public AbstractGammaObject(GammaStm stm) {
        this.stm = stm;
    }

    @Override
    public final long getVersion() {
        return version;
    }

    @Override
    public final GammaStm getStm() {
        return stm;
    }

    @Override
    public final Lock getLock() {
        return this;
    }

    public final Listeners ___removeListenersAfterWrite() {
        if (listeners == null) {
            return null;
        }

        Listeners removedListeners;
        while (true) {
            removedListeners = listeners;
            if (___unsafe.compareAndSwapObject(this, listenersOffset, removedListeners, null)) {
                return removedListeners;
            }
        }
    }

    @Override
    public final int registerChangeListener(
            final RetryLatch latch,
            final GammaRefTranlocal tranlocal,
            final GammaObjectPool pool,
            final long listenerEra) {

        if (tranlocal.isCommuting() || tranlocal.isConstructing()) {
            return REGISTRATION_NONE;
        }

        final long version = tranlocal.version;

        if (version != this.version) {
            //if it currently already contains a different version, we are done.
            latch.open(listenerEra);
            return REGISTRATION_NOT_NEEDED;
        }

        //we are going to register the listener since the current value still matches with is active.
        //But it could be that the registration completes after the write has happened.

        Listeners update = pool.takeListeners();
        //update.threadName = Thread.currentThread().getName();
        update.listener = latch;
        update.listenerEra = listenerEra;

        //we need to do this in a loop because other register thread could be contending for the same
        //listeners field.
        while (true) {
            if (version != this.version) {
                //if it currently already contains a different version, we are done.
                latch.open(listenerEra);
                return REGISTRATION_NOT_NEEDED;
            }

            //the listeners object is mutable, but as long as it isn't yet registered, this calling
            //thread has full ownership of it.
            final Listeners current = listeners;
            update.next = current;

            //lets try to register our listeners.
            final boolean registered = ___unsafe.compareAndSwapObject(this, listenersOffset, current, update);
            if (!registered) {
                //so we are contending with another register thread, so lets try it again. Since the compareAndSwap
                //didn't succeed, we know that the current thread still has exclusive ownership on the Listeners object
                //so we can try to register it again, but now with the newly found listeners
                continue;
            }

            //the registration was a success. We need to make sure that the ___version hasn't changed.
            //JMM: the volatile read of ___version can't jump in front of the unsafe.compareAndSwap.
            if (version == this.version) {
                //we are lucky, the registration was done successfully and we managed to cas the listener
                //before the update (since the update we are interested in, hasn't happened yet). This means that
                //the updating thread is now responsible for notifying the listeners. Retrieval of the most recently
                //published listener, always happens after the version is updated
                return REGISTRATION_DONE;
            }

            //the version has changed, so an interesting write has happened. No registration is needed.
            //JMM: the unsafe.compareAndSwap can't jump over the volatile read this.___version.
            //the update has taken place, we need to check if our listeners still is in place.
            //if it is, it should be removed and the listeners notified. If the listeners already has changed,
            //it is the task for the other to do the listener cleanup and notify them
            while (true) {
                update = listeners;
                final boolean removed = ___unsafe.compareAndSwapObject(this, listenersOffset, update, null);

                if (!removed) {
                    continue;
                }

                if (update != null) {
                    //we have complete ownership of the listeners that are removed, so lets open them.
                    update.openAll(pool);
                }
                return REGISTRATION_NOT_NEEDED;
            }
        }
    }


    @SuppressWarnings({"SimplifiableIfStatement"})
    @Override
    public final boolean hasReadConflict(final GammaRefTranlocal tranlocal) {
        if (tranlocal.lockMode != LOCKMODE_NONE) {
            return false;
        }

        if (hasExclusiveLock()) {
            return true;
        }

        return tranlocal.version != version;
    }

    protected final int arriveAndExclusiveLockOrBackoff() {
        for (int k = 0; k <= stm.defaultMaxRetries; k++) {
            final int arriveStatus = arriveAndExclusiveLock(stm.spinCount);

            if (arriveStatus != FAILURE) {
                return arriveStatus;
            }

            stm.defaultBackoffPolicy.delayedUninterruptible(k + 1);
        }

        return FAILURE;
    }

    //a controlled jmm problem here since identityHashCode is not synchronized/volatile/final.
    //this is the same as with the hashcode and String.
    @Override
    public final int identityHashCode() {
        int tmp = identityHashCode;
        if (tmp != 0) {
            return tmp;
        }

        tmp = System.identityHashCode(this);
        identityHashCode = tmp;
        return tmp;
    }

    public final int atomicGetLockModeAsInt() {
        final long current = orec;

        if (hasExclusiveLock(current)) {
            return LOCKMODE_EXCLUSIVE;
        }

        if (hasWriteLock(current)) {
            return LOCKMODE_WRITE;
        }

        if (getReadLockCount(current) > 0) {
            return LOCKMODE_READ;
        }

        return LOCKMODE_NONE;
    }

    @Override
    public final LockMode atomicGetLockMode() {
        switch (atomicGetLockModeAsInt()) {
            case LOCKMODE_NONE:
                return LockMode.None;
            case LOCKMODE_READ:
                return LockMode.Read;
            case LOCKMODE_WRITE:
                return LockMode.Write;
            case LOCKMODE_EXCLUSIVE:
                return LockMode.Exclusive;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public final LockMode getLockMode() {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return getLockMode(tx);
    }

    @Override
    public final LockMode getLockMode(final Transaction tx) {
        return getLockMode((GammaTransaction) tx);
    }

    public final LockMode getLockMode(final GammaTransaction tx) {
        final GammaRefTranlocal tranlocal = tx.locate((AbstractGammaRef) this);

        if (tranlocal == null) {
            return LockMode.None;
        }

        switch (tranlocal.getLockMode()) {
            case LOCKMODE_NONE:
                return LockMode.None;
            case LOCKMODE_READ:
                return LockMode.Read;
            case LOCKMODE_WRITE:
                return LockMode.Write;
            case LOCKMODE_EXCLUSIVE:
                return LockMode.Exclusive;
            default:
                throw new IllegalStateException();
        }
    }

    private static void yieldIfNeeded(final int remainingSpins) {
        if (remainingSpins % SPIN_YIELD == 0 && remainingSpins > 0) {
            //noinspection CallToThreadYield
            Thread.yield();
        }
    }

    @Override
    public final boolean tryLockAndCheckConflict(
            final GammaTransaction tx,
            final GammaRefTranlocal tranlocal,
            final int spinCount,
            final int desiredLockMode) {

        final int currentLockMode = tranlocal.getLockMode();

        //if the currentLockMode mode is higher or equal than the desired lockmode, we are done.
        if (currentLockMode >= desiredLockMode) {
            return true;
        }

        //no lock currently is acquired, lets acquire it.
        if (currentLockMode == LOCKMODE_NONE) {
            final long expectedVersion = tranlocal.version;

            //if the version already is different, there is a conflict, we are done since since the lock doesn't need to be acquired.
            if (expectedVersion != version) {
                return false;
            }

            if (tranlocal.hasDepartObligation()) {
                int result = lockAfterArrive(spinCount, desiredLockMode);
                if (result == FAILURE) {
                    return false;
                }

                if ((result & MASK_CONFLICT) != 0) {
                    tx.commitConflict = true;
                }

                if (version != expectedVersion) {
                    tranlocal.setDepartObligation(false);
                    departAfterFailureAndUnlock();
                    return false;
                }
            } else {
                //we need to arrive as well because the the tranlocal was readbiased, and no real arrive was done.
                final int result = arriveAndLock(spinCount, desiredLockMode);

                if (result == FAILURE) {
                    return false;
                }

                tranlocal.setLockMode(desiredLockMode);

                if ((result & MASK_UNREGISTERED) == 0) {
                    tranlocal.hasDepartObligation = true;
                }

                if ((result & MASK_CONFLICT) != 0) {
                    tx.commitConflict = true;
                }

                if (version != expectedVersion) {
                    return false;
                }
            }

            tranlocal.setLockMode(desiredLockMode);
            return true;
        }

        //if a readlock is acquired, we need to upgrade it to a write/exclusive-lock
        if (currentLockMode == LOCKMODE_READ) {
            if (!upgradeReadLock(spinCount, desiredLockMode == LOCKMODE_EXCLUSIVE)) {
                return false;
            }

            tranlocal.setLockMode(desiredLockMode);
            return true;
        }

        //so we have the write lock, its needs to be upgraded to a commit lock.
        if (upgradeWriteLockToExclusiveLock()) {
            //todo:
            throw new TodoException();
        }
        tranlocal.setLockMode(LOCKMODE_EXCLUSIVE);
        return true;
    }

    public final boolean waitForExclusiveLockToBecomeFree(int spinCount) {
        do {
            if (!hasExclusiveLock(orec)) {
                return true;
            }

            spinCount--;
        } while (spinCount >= 0);

        return false;
    }

    public final boolean hasWriteLock() {
        return hasWriteLock(orec);
    }

    public final boolean hasExclusiveLock() {
        return hasExclusiveLock(orec);
    }

    public final int getReadBiasedThreshold() {
        return READBIASED_THRESHOLD;
    }

    public final long getSurplus() {
        return getSurplus(orec);
    }

    public final boolean isReadBiased() {
        return isReadBiased(orec);
    }

    public final int getReadonlyCount() {
        return getReadonlyCount(orec);
    }

    public final int getReadLockCount() {
        return getReadLockCount(orec);
    }

    /**
     * Arrives. The Arrive is needed for the fast conflict detection (rich mans conflict).
     *
     * @param spinCount the maximum number of times to spin if the exclusive lock is acquired.
     * @return the arrive status.
     */
    public final int arrive(int spinCount) {
        do {
            final long current = orec;

            if (hasExclusiveLock(current)) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            long surplus = getSurplus(current);

            final boolean isReadBiased = isReadBiased(current);

            if (isReadBiased) {
                if (surplus == 0) {
                    surplus = 1;
                } else if (surplus == 1) {
                    return MASK_SUCCESS + MASK_UNREGISTERED;
                } else {
                    throw new PanicError("Surplus for a readbiased orec can never be larger than 1");
                }
            } else {
                surplus++;
            }

            final long next = setSurplus(current, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                int result = MASK_SUCCESS;

                if (isReadBiased) {
                    result += MASK_UNREGISTERED;
                }

                return result;
            }
        } while (spinCount >= 0);

        return FAILURE;
    }

    //todo: here the conflict count should be returned,
    public final boolean upgradeReadLock(int spinCount, final boolean exclusiveLock) {
        if (true) {
            throw new TodoException();
        }

        do {
            final long current = orec;

            int readLockCount = getReadLockCount(current);

            if (readLockCount == 0) {
                throw new PanicError(format("Can't update from readlock to %s if no readlocks are acquired",
                        exclusiveLock ? "exclusiveLock" : "writeLock"));
            }

            if (readLockCount > 1) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            long next = setReadLockCount(current, 0);
            if (exclusiveLock) {
                next = setExclusiveLock(next, true);
            } else {
                next = setWriteLock(next, true);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return true;
            }
        } while (spinCount >= 0);

        return false;
    }


    /**
     * Upgrades the writeLock to an exclusive lock.
     *
     * @return true if there was at least one conflict write.
     */
    public final boolean upgradeWriteLockToExclusiveLock() {
        if (true) {
            throw new TodoException();
        }

        while (true) {
            final long current = orec;

            if (hasExclusiveLock(current)) {
                return false;
            }

            if (!hasWriteLock(current)) {
                throw new PanicError("WriteLock is not acquired");
            }

            long next = setExclusiveLock(current, true);
            next = setWriteLock(next, false);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased(current) || getSurplus(current) > 1;
            }
        }
    }

    /**
     * Arrives and tries to acquire the lock. If one of them fails, there will not be any state change.
     *
     * @param spinCount the maximum number of times to spin to wait for the lock to come available.
     * @param lockMode  the desired lockmode. It isn't allowed to be LOCKMODE_NONE.
     * @return the result of this operation.
     */
    public final int arriveAndLock(int spinCount, final int lockMode) {
        assert lockMode != LOCKMODE_NONE;

        do {
            final long current = orec;

            boolean locked = lockMode == LOCKMODE_READ ? hasWriteOrExclusiveLock(current) : hasAnyLock(current);

            if (locked) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            long currentSurplus = getSurplus(current);
            long surplus = currentSurplus;
            boolean isReadBiased = isReadBiased(current);

            if (isReadBiased) {
                if (surplus == 0) {
                    surplus = 1;
                } else if (surplus > 1) {
                    throw new PanicError("Surplus is larger than 1 and orec is readbiased: " + toOrecString(current));
                }
            } else {
                surplus++;
            }

            long next = setSurplus(current, surplus);

            if (lockMode == LOCKMODE_EXCLUSIVE) {
                next = setExclusiveLock(next, true);
            } else if (lockMode == LOCKMODE_READ) {
                next = setReadLockCount(next, getReadLockCount(current) + 1);
            } else if (lockMode == LOCKMODE_WRITE) {
                next = setWriteLock(next, true);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                int result = MASK_SUCCESS;

                if (isReadBiased) {
                    result += MASK_UNREGISTERED;
                }

                if (lockMode == LOCKMODE_EXCLUSIVE && currentSurplus > 0) {
                    result += MASK_CONFLICT;
                }

                return result;
            }
        } while (spinCount >= 0);

        return FAILURE;
    }

    /**
     * Tries to acquire the exclusive lock and arrive.
     *
     * @param spinCount the maximum number of spins when it is locked.
     * @return the arrive-status.
     */
    public final int arriveAndExclusiveLock(int spinCount) {
        do {
            final long current = orec;

            if (hasAnyLock(current)) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            final long currentSurplus = getSurplus(current);
            long surplus = currentSurplus;
            boolean isReadBiased = isReadBiased(current);

            if (isReadBiased) {
                if (surplus == 0) {
                    surplus = 1;
                } else if (surplus > 1) {
                    throw new PanicError("Surplus is larger than 2: " + toOrecString(current));
                }
            } else {
                surplus++;
            }

            long next = setSurplus(current, surplus);
            next = setExclusiveLock(next, true);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                int result = MASK_SUCCESS;

                if (isReadBiased) {
                    result += MASK_UNREGISTERED;
                }

                if (currentSurplus > 0) {
                    result += MASK_CONFLICT;
                }

                return result;
            }
        } while (spinCount >= 0);

        return FAILURE;
    }

    /**
     * Arrives and tries to acquire the lock. If one of them fails, there will not be any state change.
     *
     * @param spinCount the maximum number of times to spin if a lock is acquired.
     * @param lockMode  the desired lockMode. This is not allowed to be LOCKMODE_NONE.
     * @return
     */
    public final int lockAfterArrive(int spinCount, final int lockMode) {
        assert lockMode != LOCKMODE_NONE;

        do {
            final long current = orec;

            if (isReadBiased(current)) {
                throw new PanicError("Orec is readbiased " + toOrecString(current));
            }

            boolean locked = lockMode == LOCKMODE_READ ? hasWriteOrExclusiveLock(current) : hasAnyLock(current);

            if (locked) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            final long currentSurplus = getSurplus(current);
            if (currentSurplus == 0) {
                throw new PanicError("There is no surplus (so if it didn't do a read before)" + toOrecString(current));
            }

            long next = current;
            if (lockMode == LOCKMODE_READ) {
                next = setReadLockCount(next, getReadLockCount(current) + 1);
            } else if (lockMode == LOCKMODE_EXCLUSIVE) {
                next = setExclusiveLock(next, true);
            } else {
                next = setWriteLock(current, true);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                int result = MASK_SUCCESS;

                if (lockMode == LOCKMODE_EXCLUSIVE && currentSurplus > 1) {
                    result += MASK_CONFLICT;
                }

                return result;
            }
        } while (spinCount >= 0);

        return FAILURE;
    }

    /**
     * Departs after a successful read is done and no lock was acquired.
     * <p/>
     * This call increased the readonly count. If the readonly count threshold is reached, the orec is
     * made readbiased and the readonly count is set to 0.
     */
    public final void departAfterReading() {
        while (true) {
            final long current = orec;

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError("There is no surplus " + toOrecString(current));
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError("Orec is readbiased " + toOrecString(current));
            }

            int readonlyCount = getReadonlyCount(current);
            if (readonlyCount < READBIASED_THRESHOLD) {
                readonlyCount++;
            }

            if (surplus <= 1 && hasAnyLock(current)) {
                throw new PanicError("There is not enough surplus " + toOrecString(current));
            }

            surplus--;
            final boolean hasExclusiveLock = hasExclusiveLock(current);
            if (!hasExclusiveLock && surplus == 0 && readonlyCount == READBIASED_THRESHOLD) {
                isReadBiased = true;
                readonlyCount = 0;
            }

            long next = setIsReadBiased(current, isReadBiased);
            next = setReadonlyCount(next, readonlyCount);
            next = setSurplus(next, surplus);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    /**
     * Departs after a successful read is done and release the lock (it doesn't matter which lock is acquired as long is
     * it is a read/write/exclusive lock.
     * <p/>
     * This method increases the readonly count of the orec and upgraded from update-biased to
     * readbiased if the READBIASED_THRESHOLD is reached (also the readonly count is set to zero
     * if that happens).
     */
    public final void departAfterReadingAndUnlock() {
        while (true) {
            final long current = orec;

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError("There is no surplus: " + toOrecString(current));
            }

            int readLockCount = getReadLockCount(current);

            if (readLockCount == 0 && !hasWriteOrExclusiveLock(current)) {
                throw new PanicError("No Lock acquired " + toOrecString(current));
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError("Orec is readbiased " + toOrecString(current));
            }

            int readonlyCount = getReadonlyCount(current);

            surplus--;

            if (readonlyCount < READBIASED_THRESHOLD) {
                readonlyCount++;
            }

            if (surplus == 0 && readonlyCount == READBIASED_THRESHOLD) {
                isReadBiased = true;
                readonlyCount = 0;
            }

            //todo: if exclusive lock is acquired and no readers/then cheap write

            long next = current;
            if (readLockCount > 0) {
                next = setReadLockCount(next, readLockCount - 1);
            } else {
                next = setExclusiveLock(next, false);
                next = setWriteLock(next, false);
            }

            next = setIsReadBiased(next, isReadBiased);
            next = setReadonlyCount(next, readonlyCount);
            next = setSurplus(next, surplus);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public final void departAfterUpdateAndUnlock() {
        while (true) {
            final long current = orec;

            if (!hasExclusiveLock(current)) {
                throw new PanicError(
                        "Can't departAfterUpdateAndUnlock if the commit lock is not acquired " + toOrecString(current));
            }

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError(
                        "Can't departAfterUpdateAndUnlock is there is no surplus " + toOrecString(current));
            }

            if (isReadBiased(current)) {
                if (surplus > 1) {
                    throw new PanicError(
                            "The surplus can never be larger than 1 if readBiased " + toOrecString(current));
                }

                //there always is a conflict when a readbiased orec is updated.
                surplus = 0;
            } else {
                surplus--;
            }

            if (surplus == 0) {
                orec = 0;
                return;
            }

            final long next = setSurplus(0, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    /**
     * Departs after a transaction fails and has an arrive on this Orec. It doesn't matter what the lock level
     * is, as long as it is higher than LOCKMODE_NONE. This call can safely be made on a read or update biased
     * ref.
     */
    public final void departAfterFailureAndUnlock() {
        while (true) {
            final long current = orec;

            //-1 indicates write or commit lock, value bigger than 0 indicates readlock
            int lockMode;

            if (hasWriteOrExclusiveLock(current)) {
                lockMode = -1;
            } else {
                lockMode = getReadLockCount(current);
            }

            if (lockMode == 0) {
                throw new PanicError(
                        "No lock was not acquired " + toOrecString(current));
            }

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError(
                        "There is no surplus " + toOrecString(current));
            }

            //we can only decrease the surplus if it is not read biased. Because with a read biased
            //orec, we have no idea how many readers there are.
            if (!isReadBiased(current)) {
                surplus--;
            }

            long next = setSurplus(current, surplus);
            if (lockMode == -1) {
                next = setExclusiveLock(next, false);
                next = setWriteLock(next, false);
            } else {
                next = setReadLockCount(next, lockMode - 1);
            }

            //todo: if a there is no surplus and exclusive lock is held, the same trick as with the releaseAfterUpdate
            //can be applied that prevent a cas.

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    /**
     * Departs after failure.
     */
    public final void departAfterFailure() {
        while (true) {
            final long current = orec;

            if (isReadBiased(current)) {
                throw new PanicError("Orec is readbiased:" + toOrecString(current));
            }

            long surplus = getSurplus(current);

            if (hasExclusiveLock(current)) {
                if (surplus < 2) {
                    throw new PanicError(
                            "there must be at least 2 readers, the thread that acquired the lock, " +
                                    "and the calling thread " + toOrecString(current));
                }
            } else if (surplus == 0) {
                throw new PanicError("There is no surplus " + toOrecString(current));
            }

            surplus--;

            long next = setSurplus(current, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public final void unlockByUnregistered() {
        while (true) {
            final long current = orec;

            //-1 indicates write or commit lock, value bigger than 0 indicates readlock
            if (!isReadBiased(current)) {
                throw new PanicError(
                        "Can't ___unlockByReadBiased when it is not readbiased " + toOrecString(current));
            }

            int lockMode;
            if (hasWriteOrExclusiveLock(current)) {
                lockMode = -1;
            } else {
                lockMode = getReadLockCount(current);
            }

            if (lockMode == 0) {
                throw new PanicError("No Lock " + toOrecString(current));
            }

            if (getSurplus(current) > 1) {
                throw new PanicError("Surplus for readbiased orec larger than 1 " + toOrecString(current));
            }

            //todo: cheap write

            long next = current;
            if (lockMode > 0) {
                next = setReadLockCount(next, lockMode - 1);
            } else {
                next = setExclusiveLock(next, false);
                next = setWriteLock(next, false);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public final String ___toOrecString() {
        return toOrecString(orec);
    }

    public static long setReadLockCount(final long value, final long readLockCount) {
        return (value & ~MASK_OREC_READLOCKS) | (readLockCount << 40);
    }

    public static int getReadLockCount(final long value) {
        return (int) ((value & MASK_OREC_READLOCKS) >> 40);
    }

    public static long setExclusiveLock(final long value, final boolean exclusiveLock) {
        return (value & ~MASK_OREC_EXCLUSIVELOCK) | ((exclusiveLock ? 1L : 0L) << 63);
    }

    public static boolean hasWriteOrExclusiveLock(final long value) {
        return ((value & (MASK_OREC_EXCLUSIVELOCK + MASK_OREC_UPDATELOCK)) != 0);
    }

    public static boolean hasAnyLock(final long value) {
        return ((value & (MASK_OREC_EXCLUSIVELOCK + MASK_OREC_UPDATELOCK + MASK_OREC_READLOCKS)) != 0);
    }

    public static boolean hasExclusiveLock(final long value) {
        return (value & MASK_OREC_EXCLUSIVELOCK) != 0;
    }

    public static boolean isReadBiased(final long value) {
        return (value & MASK_OREC_READBIASED) != 0;
    }

    public static long setIsReadBiased(final long value, final boolean isReadBiased) {
        return (value & ~MASK_OREC_READBIASED) | ((isReadBiased ? 1L : 0L) << 61);
    }

    public static boolean hasWriteLock(final long value) {
        return (value & MASK_OREC_UPDATELOCK) != 0;
    }

    public static long setWriteLock(final long value, final boolean updateLock) {
        return (value & ~MASK_OREC_UPDATELOCK) | ((updateLock ? 1L : 0L) << 62);
    }

    public static int getReadonlyCount(final long value) {
        return (int) (value & MASK_OREC_READONLY_COUNT);
    }

    public static long setReadonlyCount(final long value, final int readonlyCount) {
        return (value & ~MASK_OREC_READONLY_COUNT) | readonlyCount;
    }

    public static long setSurplus(final long value, final long surplus) {
        return (value & ~MASK_OREC_SURPLUS) | (surplus << 10);
    }

    public static long getSurplus(final long value) {
        return (value & MASK_OREC_SURPLUS) >> 10;
    }

    private static String toOrecString(final long value) {
        return format(
                "Orec(hasExclusiveLock=%s, hasWriteLock=%s, readLocks=%s, surplus=%s, isReadBiased=%s, readonlyCount=%s)",
                hasExclusiveLock(value),
                hasWriteLock(value),
                getReadLockCount(value),
                getSurplus(value),
                isReadBiased(value),
                getReadonlyCount(value));
    }
}

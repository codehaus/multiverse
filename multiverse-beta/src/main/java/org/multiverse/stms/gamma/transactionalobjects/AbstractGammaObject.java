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

public abstract class AbstractGammaObject implements GammaObject, Lock {

    //it is important that the maximum threshold is not larger than 1023 (there are 10 bits for the readonly count)
    private static final int READBIASED_THRESHOLD = 16;

    private static final long BITMASK_EXCLUSIVELOCK = 0x8000000000000000L;
    private static final long BITMASK_UPDATELOCK = 0x4000000000000000L;
    private static final long BITMASK_READBIASED = 0x2000000000000000L;
    private static final long BITMASK_READLOCKS = 0x1FFFFF0000000000L;
    private static final long BITMASK_SURPLUS = 0x000000FFFFFFFE00L;
    private static final long BITMASK_READONLY_COUNT = 0x00000000000003FFL;

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
    protected volatile Listeners listeners;

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

    protected final Listeners ___removeListenersAfterWrite() {
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

    @Override
    public final boolean tryAcquire(final LockMode desiredLockMode) {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return tryAcquire(tx, desiredLockMode);
    }

    @Override
    public final boolean tryAcquire(final Transaction tx, final LockMode desiredLockMode) {
        return tryAcquire((GammaTransaction) tx, desiredLockMode);
    }

    public final boolean tryAcquire(final GammaTransaction tx, final LockMode desiredLockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortTryAcquireOnBadStatus(this);
        }

        if (desiredLockMode == null) {
            throw tx.abortTryAcquireOnNullLockMode(this);
        }

        final GammaRefTranlocal tranlocal = tx.locate((AbstractGammaRef) this);

        final int currentLockMode = tranlocal == null ? LOCKMODE_NONE : tranlocal.getLockMode();

        if (currentLockMode >= desiredLockMode.asInt()) {
            return true;
        }

        switch (currentLockMode) {
            case LOCKMODE_NONE:
                break;
            case LOCKMODE_READ:
                break;
            case LOCKMODE_WRITE:
                break;
            case LOCKMODE_EXCLUSIVE:
                return true;
            default:
                throw new IllegalStateException();
        }

        throw new TodoException();
    }

    @Override
    public final void acquire(final LockMode desiredLockMode) {
        final GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        acquire(tx, desiredLockMode);
    }

    @Override
    public final void acquire(final Transaction tx, final LockMode desiredLockMode) {
        acquire((GammaTransaction) tx, desiredLockMode);
    }

    public final void acquire(final GammaTransaction tx, final LockMode lockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (lockMode == null) {
            throw tx.abortAcquireOnNullLockMode(this);
        }

        openForRead(tx, lockMode.asInt());
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    @Override
    public final boolean hasReadConflict(final GammaRefTranlocal tranlocal) {
        if (tranlocal.getLockMode() != LOCKMODE_NONE) {
            return false;
        }

        if (hasExclusiveLock()) {
            return true;
        }

        return tranlocal.version != version;
    }

    protected final int arriveAndAcquireExclusiveLockOrBackoff() {
        for (int k = 0; k <= stm.defaultMaxRetries; k++) {
            final int arriveStatus = tryExclusiveLockAndArrive(stm.spinCount);

            if (arriveStatus != ARRIVE_LOCK_NOT_FREE) {
                return arriveStatus;
            }

            stm.defaultBackoffPolicy.delayedUninterruptible(k + 1);
        }

        return ARRIVE_LOCK_NOT_FREE;
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
        if (remainingSpins % ___SpinYield == 0 && remainingSpins > 0) {
            //noinspection CallToThreadYield
            Thread.yield();
        }
    }

    @Override
    public final boolean tryLockAndCheckConflict(
            final int spinCount,
            final GammaRefTranlocal tranlocal,
            final int desiredLockMode) {

        final int currentLockMode = tranlocal.getLockMode();

        //if the currentLockMode mode is higher or equal than the desired lockmode, we are done.
        if (currentLockMode >= desiredLockMode) {
            return true;
        }

        //no lock currently is acquired, lets acquire it.
        if (currentLockMode == LOCKMODE_NONE) {
            long expectedVersion = tranlocal.version;

            //if the version already is different, and there is a conflict, we are done since since the lock doesn't need to be acquired.
            if (expectedVersion != version) {
                return false;
            }

            if (!tranlocal.hasDepartObligation()) {
                //we need to arrive as well because the the tranlocal was readbiased, and no real arrive was done.
                final int arriveStatus = tryLockAndArrive(spinCount, desiredLockMode);

                if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                    return false;
                }

                if (arriveStatus == ARRIVE_NORMAL) {
                    tranlocal.setDepartObligation(true);
                }
            } else if (!tryLockAfterNormalArrive(spinCount, desiredLockMode)) {
                return false;
            }

            tranlocal.setLockMode(desiredLockMode);

            //if the version already is different, we are done since we know that there is a conflict.
            return version == expectedVersion;
        }

        //if a readlock is acquired, we need to upgrade it.
        if (currentLockMode == LOCKMODE_READ) {
            if (!tryUpgradeFromReadLock(spinCount, desiredLockMode == LOCKMODE_EXCLUSIVE)) {
                return false;
            }

            tranlocal.setLockMode(desiredLockMode);
            return true;
        }

        //so we have the write lock, its needs to be upgraded to a commit lock.
        upgradeToExclusiveLock();
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
                    return ARRIVE_UNREGISTERED;
                } else {
                    throw new PanicError("Surplus for a readbiased orec can never be larger than 1");
                }
            } else {
                surplus++;
            }

            final long next = setSurplus(current, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_UNREGISTERED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }

    public final boolean tryUpgradeFromReadLock(int spinCount, final boolean exclusiveLock) {
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

    public final void upgradeToExclusiveLock() {
        while (true) {
            final long current = orec;

            if (hasExclusiveLock(current)) {
                return;
            }

            if (!hasWriteLock(current)) {
                throw new PanicError("Can't upgradeToExclusiveLock is the updateLock is not acquired");
            }

            long next = setExclusiveLock(current, true);
            next = setWriteLock(next, false);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public final int tryLockAndArrive(int spinCount, final int lockMode) {
        do {
            final long current = orec;

            boolean locked = lockMode == LOCKMODE_READ ? hasWriteOrExclusiveLock(current) : hasAnyLock(current);

            if (locked) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            long surplus = getSurplus(current);
            boolean isReadBiased = isReadBiased(current);

            if (isReadBiased) {
                if (surplus == 0) {
                    surplus = 1;
                } else if (surplus > 1) {
                    throw new PanicError(
                            "Can't tryLockAndArrive; surplus is larger than 1 and orec is readbiased: " + toOrecString(current));
                }
            } else {
                surplus++;
            }

            long next = setSurplus(current, surplus);

            if (lockMode != LOCKMODE_NONE) {
                if (lockMode == LOCKMODE_READ) {
                    next = setReadLockCount(next, getReadLockCount(current) + 1);
                } else if (lockMode == LOCKMODE_WRITE) {
                    next = setWriteLock(next, true);
                } else if (lockMode == LOCKMODE_EXCLUSIVE) {
                    next = setExclusiveLock(next, true);
                }
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_UNREGISTERED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }

    public final int tryExclusiveLockAndArrive(int spinCount) {
        do {
            final long current = orec;

            if (hasAnyLock(current)) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            long surplus = getSurplus(current);
            boolean isReadBiased = isReadBiased(current);

            if (isReadBiased) {
                if (surplus == 0) {
                    surplus = 1;
                } else if (surplus > 1) {
                    throw new PanicError(
                            "Can't arriveAndLockForUpdate; surplus is larger than 2: " + toOrecString(current));
                }
            } else {
                surplus++;
            }

            long next = setSurplus(current, surplus);
            next = setExclusiveLock(next, true);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_UNREGISTERED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }

    public final boolean tryLockAfterNormalArrive(int spinCount, final int lockMode) {
        do {
            final long current = orec;

            if (isReadBiased(current)) {
                throw new PanicError("Can't tryLockAfterNormalArrive of the orec is readbiased " + toOrecString(current));
            }

            boolean locked = lockMode == LOCKMODE_READ ? hasWriteOrExclusiveLock(current) : hasAnyLock(current);

            if (locked) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            if (getSurplus(current) == 0) {
                throw new PanicError(
                        "Can't acquire any Lock is there is no surplus (so if it didn't do a read before)" +
                                toOrecString(current));
            }

            long next = current;
            if (lockMode != LOCKMODE_NONE) {
                if (lockMode == LOCKMODE_READ) {
                    next = setReadLockCount(next, getReadLockCount(current) + 1);
                } else if (lockMode == LOCKMODE_EXCLUSIVE) {
                    next = setExclusiveLock(next, true);
                } else {
                    next = setWriteLock(current, true);
                }
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return true;
            }
        } while (spinCount >= 0);

        return false;
    }

    public final void departAfterReading() {
        while (true) {
            final long current = orec;

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError("Can't depart if there is no surplus " + toOrecString(current));
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError("Can't depart from a readbiased orec " + toOrecString(current));
            }

            int readonlyCount = getReadonlyCount(current);
            if (readonlyCount < READBIASED_THRESHOLD) {
                readonlyCount++;
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

    public final void departAfterReadingAndUnlock() {
        while (true) {
            final long current = orec;

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError(
                        "Can't ___departAfterReadingAndUnlock if there is no surplus: " + toOrecString(current));
            }

            int readLockCount = getReadLockCount(current);

            if (readLockCount == 0 && !hasWriteOrExclusiveLock(current)) {
                throw new PanicError(
                        "Can't ___departAfterReadingAndUnlock if the lock is not acquired " + toOrecString(current));
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError(
                        "Can't ___departAfterReadingAndUnlock when readbiased orec " + toOrecString(current));
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

    public final long departAfterUpdateAndUnlock() {
        boolean conflictSend = false;
        while (true) {
            final long current = orec;

            if (!hasExclusiveLock(current)) {
                throw new PanicError(
                        "Can't ___departAfterUpdateAndUnlock is the commit lock is not acquired " + toOrecString(current));
            }

            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError(
                        "Can't ___departAfterUpdateAndUnlock is there is no surplus " + toOrecString(current));
            }

            boolean conflict;
            if (isReadBiased(current)) {
                if (surplus > 1) {
                    throw new PanicError(
                            "The surplus can never be larger than 1 if readBiased " + toOrecString(current));
                }

                //there always is a conflict when a readbiased orec is updated.
                conflict = true;
                surplus = 0;
            } else {
                surplus--;
                conflict = surplus > 0;
            }

            if (conflict && !conflictSend) {
                stm.globalConflictCounter.signalConflict(this);
                conflictSend = true;
            }

            if (surplus == 0) {
                orec = 0;
                return surplus;
            }

            final long next = setSurplus(0, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return surplus;
            }
        }
    }

    public final long departAfterFailureAndUnlock() {
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
                        "Can't ___departAfterFailureAndUnlock if the lock was not acquired " + toOrecString(current));
            }

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError(
                        "Can't ___departAfterFailureAndUnlock if there is no surplus " + toOrecString(current));
            }

            //we can only decrease the surplus if it is not read biased. Because with a read biased
            //orec, we have no idea how many readers there are.
            if (isReadBiased(current)) {
                if (surplus > 1) {
                    throw new PanicError(
                            "Can't ___departAfterFailureAndUnlock with a surplus larger than 1 if " +
                                    "the orec is read biased " + toOrecString(current));
                }
            } else {
                surplus--;
            }

            long next = setSurplus(current, surplus);
            if (lockMode == -1) {
                next = setExclusiveLock(next, false);
                next = setWriteLock(next, false);
            } else {
                next = setReadLockCount(next, lockMode - 1);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return surplus;
            }
        }
    }

    public final void departAfterFailure() {
        while (true) {
            final long current = orec;

            if (isReadBiased(current)) {
                throw new PanicError("Can't departAfterFailure when orec is readbiased:" + toOrecString(current));
            }

            long surplus = getSurplus(current);

            if (hasExclusiveLock(current)) {
                if (surplus < 2) {
                    throw new PanicError(
                            "there must be at least 2 readers, the thread that acquired the lock, " +
                                    "and the calling thread " + toOrecString(current));
                }
            } else {
                if (surplus == 0) {
                    throw new PanicError(
                            "Can't departAfterFailure if there is no surplus " + toOrecString(current));
                }
            }
            surplus--;

            long next = setSurplus(current, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public final void unlockWhenUnregistered() {
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
                throw new PanicError(
                        "Can't ___unlockByReadBiased if it isn't locked " + toOrecString(current));
            }


            if (getSurplus(current) > 1) {
                throw new PanicError(
                        "Surplus for a readbiased orec never can be larger than 1 " + toOrecString(current));
            }

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
        return (value & ~BITMASK_READLOCKS) | (readLockCount << 40);
    }

    public static int getReadLockCount(final long value) {
        return (int) ((value & BITMASK_READLOCKS) >> 40);
    }

    public static long setExclusiveLock(final long value, final boolean exclusiveLock) {
        return (value & ~BITMASK_EXCLUSIVELOCK) | ((exclusiveLock ? 1L : 0L) << 63);
    }

    public static boolean hasWriteOrExclusiveLock(final long value) {
        return ((value & (BITMASK_EXCLUSIVELOCK + BITMASK_UPDATELOCK)) != 0);
    }

    public static boolean hasAnyLock(final long value) {
        return ((value & (BITMASK_EXCLUSIVELOCK + BITMASK_UPDATELOCK + BITMASK_READLOCKS)) != 0);
    }

    public static boolean hasExclusiveLock(final long value) {
        return (value & BITMASK_EXCLUSIVELOCK) != 0;
    }

    public static boolean isReadBiased(final long value) {
        return (value & BITMASK_READBIASED) != 0;
    }

    public static long setIsReadBiased(final long value, final boolean isReadBiased) {
        return (value & ~BITMASK_READBIASED) | ((isReadBiased ? 1L : 0L) << 61);
    }

    public static boolean hasWriteLock(final long value) {
        return (value & BITMASK_UPDATELOCK) != 0;
    }

    public static long setWriteLock(final long value, final boolean updateLock) {
        return (value & ~BITMASK_UPDATELOCK) | ((updateLock ? 1L : 0L) << 62);
    }

    public static int getReadonlyCount(final long value) {
        return (int) (value & BITMASK_READONLY_COUNT);
    }

    public static long setReadonlyCount(final long value, final int readonlyCount) {
        return (value & ~BITMASK_READONLY_COUNT) | readonlyCount;
    }

    public static long setSurplus(final long value, final long surplus) {
        return (value & ~BITMASK_SURPLUS) | (surplus << 10);
    }

    public static long getSurplus(final long value) {
        return (value & BITMASK_SURPLUS) >> 10;
    }

    private static String toOrecString(final long value) {
        return format(
                "Orec(hasExclusiveLock=%s, hasUpdateLock=%s, readLocks=%s, surplus=%s, isReadBiased=%s, readonlyCount=%s)",
                hasExclusiveLock(value),
                hasWriteLock(value),
                getReadonlyCount(value),
                getSurplus(value),
                isReadBiased(value),
                getReadonlyCount(value));
    }
}

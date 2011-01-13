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

    private static final long BITMASK_COMMITLOCK = 0x8000000000000000L;
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
                    AbstractGammaObject.class.getDeclaredField("___listeners"));
            valueOffset = ___unsafe.objectFieldOffset(
                    AbstractGammaObject.class.getDeclaredField("___orec"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public final GammaStm ___stm;

    protected volatile Listeners ___listeners;

    public volatile long version;

    private volatile long ___orec;

    //This field has a controlled JMM problem (just like the hashcode of String).
    protected int ___identityHashCode;

    public AbstractGammaObject(GammaStm stm) {
        this.___stm = stm;
    }

    @Override
    public final long getVersion() {
        return version;
    }

    @Override
    public final GammaStm getStm() {
        return ___stm;
    }

    @Override
    public final Lock getLock() {
        return this;
    }

    protected final Listeners ___removeListenersAfterWrite() {
        if (___listeners == null) {
            return null;
        }

        Listeners removedListeners;
        while (true) {
            removedListeners = ___listeners;
            if (___unsafe.compareAndSwapObject(this, listenersOffset, removedListeners, null)) {
                return removedListeners;
            }
        }
    }

    @Override
    public final int registerChangeListener(
            final RetryLatch latch,
            final GammaTranlocal tranlocal,
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
        update.threadName = Thread.currentThread().getName();
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
            final Listeners current = ___listeners;
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
                update = ___listeners;
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
    public boolean tryAcquire(LockMode desiredLockMode) {
        GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return tryAcquire(tx, desiredLockMode);
    }

    @Override
    public boolean tryAcquire(Transaction tx, LockMode desiredLockMode) {
        return tryAcquire((GammaTransaction) tx, desiredLockMode);
    }

    public boolean tryAcquire(GammaTransaction tx, LockMode desiredLockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (tx.status != TX_ACTIVE) {
            throw tx.abortTryAcquireOnBadStatus();
        }

        if (desiredLockMode == null) {
            throw tx.abortTryAcquireOnNullLockMode();
        }

        GammaTranlocal tranlocal = tx.locate(this);

        int currentLockMode = tranlocal == null ? LOCKMODE_NONE : tranlocal.getLockMode();

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
            case LOCKMODE_COMMIT:
                return true;
            default:
                throw new IllegalStateException();
        }

        throw new TodoException();
    }

    @Override
    public void acquire(LockMode desiredLockMode) {
        GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();
        if (tx == null) {
            throw new TransactionRequiredException();
        }

        acquire(tx, desiredLockMode);
    }

    @Override
    public void acquire(Transaction tx, LockMode desiredLockMode) {
        acquire((GammaTransaction) tx, desiredLockMode);
    }

    public void acquire(GammaTransaction tx, LockMode lockMode) {
        if (tx == null) {
            throw new NullPointerException();
        }

        if (lockMode == null) {
            throw tx.abortOnNullLockMode();
        }

        openForRead(tx, lockMode.asInt());
    }

    @Override
    public final boolean hasReadConflict(final GammaTranlocal tranlocal) {
        if (tranlocal.getLockMode() != LOCKMODE_NONE) {
            return false;
        }

        if (hasCommitLock()) {
            return true;
        }

        return tranlocal.version != version;
    }

    protected final int arriveAndCommitLockOrBackoff() {
        for (int k = 0; k <= ___stm.defaultMaxRetries; k++) {
            final int arriveStatus = tryCommitLockAndArrive(___stm.spinCount);

            if (arriveStatus != ARRIVE_LOCK_NOT_FREE) {
                return arriveStatus;
            }

            ___stm.defaultBackoffPolicy.delayedUninterruptible(k + 1);
        }

        return ARRIVE_LOCK_NOT_FREE;
    }

    //a controlled jmm problem here since identityHashCode is not synchronized/volatile/final.
    //this is the same as with the hashcode and String.
    @Override
    public final int identityHashCode() {
        int tmp = ___identityHashCode;
        if (tmp != 0) {
            return tmp;
        }

        tmp = System.identityHashCode(this);
        ___identityHashCode = tmp;
        return tmp;
    }

    public int atomicGetLockModeAsInt() {
        long current = ___orec;
        if (hasCommitLock(current)) {
            return LOCKMODE_COMMIT;
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
    public LockMode atomicGetLockMode() {
        switch (atomicGetLockModeAsInt()) {
            case LOCKMODE_NONE:
                return LockMode.None;
            case LOCKMODE_READ:
                return LockMode.Read;
            case LOCKMODE_WRITE:
                return LockMode.Write;
            case LOCKMODE_COMMIT:
                return LockMode.Commit;
            default:
                throw new IllegalStateException();
        }
    }

    @Override
    public LockMode getLockMode() {
        GammaTransaction tx = (GammaTransaction) getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException();
        }

        return getLockMode(tx);
    }

    @Override
    public LockMode getLockMode(Transaction tx) {
        return getLockMode((GammaTransaction) tx);
    }

    public LockMode getLockMode(GammaTransaction tx) {
        GammaTranlocal tranlocal = tx.locate(this);
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
            case LOCKMODE_COMMIT:
                return LockMode.Commit;
            default:
                throw new IllegalStateException();
        }
    }

    private void yieldIfNeeded(int remainingSpins) {
        if (remainingSpins % ___SpinYield == 0 && remainingSpins > 0) {
            Thread.yield();
        }
    }

    @Override
    public void releaseAfterFailure(GammaTranlocal tranlocal, GammaObjectPool pool) {
        if (tranlocal.headCallable != null) {
            CallableNode node = tranlocal.headCallable;
            do {
                CallableNode next = node.next;
                pool.putCallableNode(node);
                node = next;
            } while (node != null);
            tranlocal.headCallable = null;
        }

        if (tranlocal.hasDepartObligation()) {
            if (tranlocal.getLockMode() != LOCKMODE_NONE) {
                departAfterFailureAndUnlock();
                tranlocal.setLockMode(LOCKMODE_NONE);
            } else {
                departAfterFailure();
            }
            tranlocal.setDepartObligation(false);
        } else if (tranlocal.getLockMode() != LOCKMODE_NONE) {
            unlockWhenUnregistered();
            tranlocal.setLockMode(LOCKMODE_NONE);
        }

        tranlocal.owner = null;
    }

    @Override
    public void releaseAfterUpdate(GammaTranlocal tranlocal, GammaObjectPool pool) {
        departAfterUpdateAndUnlock();
        tranlocal.setLockMode(LOCKMODE_NONE);
        tranlocal.owner = null;
        tranlocal.setDepartObligation(false);
    }

    @Override
    public void releaseAfterReading(GammaTranlocal tranlocal, GammaObjectPool pool) {
        if (tranlocal.hasDepartObligation()) {
            if (tranlocal.getLockMode() != LOCKMODE_NONE) {
                departAfterReadingAndUnlock();
                tranlocal.setLockMode(LOCKMODE_NONE);
            } else {
                departAfterReading();
            }
            tranlocal.setDepartObligation(false);
        } else if (tranlocal.getLockMode() != LOCKMODE_NONE) {
            unlockWhenUnregistered();
            tranlocal.setLockMode(LOCKMODE_NONE);
        }

        tranlocal.owner = null;
    }

    @Override
    public final boolean tryLockAndCheckConflict(
            final int spinCount,
            final GammaTranlocal tranlocal,
            final int desiredLockMode) {

        final int currentLockMode = tranlocal.getLockMode();

        //if the currentlock mode is higher or equal than the desired lockmode, we are done.
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
            if (!tryUpgradeFromReadLock(spinCount, desiredLockMode == LOCKMODE_COMMIT)) {
                return false;
            }

            tranlocal.setLockMode(desiredLockMode);
            return true;
        }

        //so we have the write lock, its needs to be upgraded to a commit lock.
        upgradeToCommitLock();
        tranlocal.setLockMode(LOCKMODE_COMMIT);
        return true;
    }

    public final boolean waitForNoCommitLock(int spinCount) {
        do {
            if (!hasCommitLock(___orec)) {
                return true;
            }

            spinCount--;
        } while (spinCount >= 0);

        return false;
    }

    public final boolean hasWriteLock() {
        return hasWriteLock(___orec);
    }

    public final boolean hasCommitLock() {
        return hasCommitLock(___orec);
    }

    public final int getReadBiasedThreshold() {
        return READBIASED_THRESHOLD;
    }

    public final long getSurplus() {
        return getSurplus(___orec);
    }

    public final boolean isReadBiased() {
        return isReadBiased(___orec);
    }

    public final int getReadonlyCount() {
        return getReadonlyCount(___orec);
    }

    public int getReadLockCount() {
        return getReadLockCount(___orec);
    }

    public final int arrive(int spinCount) {
        do {
            final long current = ___orec;

            if (hasCommitLock(current)) {
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

            long next = setSurplus(current, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_UNREGISTERED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }

    public boolean tryUpgradeFromReadLock(int spinCount, boolean commitLock) {
        do {
            final long current = ___orec;

            int readLockCount = getReadLockCount(current);

            if (readLockCount == 0) {
                throw new PanicError();
            }

            if (readLockCount > 1) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            long next = setReadLockCount(current, 0);
            if (commitLock) {
                next = setCommitLock(next, true);
            } else {
                next = setWriteLock(next, true);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return true;
            }
        } while (spinCount >= 0);

        return false;
    }

    public void upgradeToCommitLock() {
        while (true) {
            final long current = ___orec;

            if (hasCommitLock(current)) {
                return;
            }

            if (!hasWriteLock(current)) {
                throw new PanicError("Can't upgradeToCommitLock is the updateLock is not acquired");
            }

            long next = setCommitLock(current, true);
            next = setWriteLock(next, false);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public final int tryLockAndArrive(int spinCount, final int lockMode) {
        do {
            final long current = ___orec;

            boolean locked = lockMode == LOCKMODE_READ ? hasWriteOrCommitLock(current) : hasAnyLock(current);

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
                } else if (lockMode == LOCKMODE_COMMIT) {
                    next = setCommitLock(next, true);
                }
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_UNREGISTERED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }

    public final int tryCommitLockAndArrive(int spinCount) {
        do {
            final long current = ___orec;

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
            next = setCommitLock(next, true);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_UNREGISTERED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }


    public final boolean tryLockAfterNormalArrive(int spinCount, final int lockMode) {
        do {
            final long current = ___orec;

            if (isReadBiased(current)) {
                throw new PanicError("Can't tryLockAfterNormalArrive of the orec is readbiased " + toOrecString(current));
            }

            boolean locked = lockMode == LOCKMODE_READ ? hasWriteOrCommitLock(current) : hasAnyLock(current);

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
                } else if (lockMode == LOCKMODE_COMMIT) {
                    next = setCommitLock(next, true);
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
            final long current = ___orec;

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
            final boolean hasCommitLock = hasCommitLock(current);
            if (!hasCommitLock && surplus == 0 && readonlyCount == READBIASED_THRESHOLD) {
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
            final long current = ___orec;

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError(
                        "Can't ___departAfterReadingAndUnlock if there is no surplus: " + toOrecString(current));
            }

            int readLockCount = getReadLockCount(current);

            if (readLockCount == 0 && !hasWriteOrCommitLock(current)) {
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
                next = setCommitLock(next, false);
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
            final long current = ___orec;

            if (!hasCommitLock(current)) {
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
                ___stm.globalConflictCounter.signalConflict(this);
                conflictSend = true;
            }

            if (surplus == 0) {
                ___orec = 0;
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
            final long current = ___orec;

            //-1 indicates write or commit lock, value bigger than 0 indicates readlock
            int lockMode;

            if (hasWriteOrCommitLock(current)) {
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
                next = setCommitLock(next, false);
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
            final long current = ___orec;

            if (isReadBiased(current)) {
                throw new PanicError("Can't departAfterFailure when orec is readbiased:" + toOrecString(current));
            }

            long surplus = getSurplus(current);

            if (hasCommitLock(current)) {
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
            final long current = ___orec;

            //-1 indicates write or commit lock, value bigger than 0 indicates readlock
            if (!isReadBiased(current)) {
                throw new PanicError(
                        "Can't ___unlockByReadBiased when it is not readbiased " + toOrecString(current));
            }

            int lockMode;
            if (hasWriteOrCommitLock(current)) {
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
                next = setCommitLock(next, false);
                next = setWriteLock(next, false);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public final String ___toOrecString() {
        return toOrecString(___orec);
    }

    public static long setReadLockCount(final long value, final long readLockCount) {
        return (value & ~BITMASK_READLOCKS) | (readLockCount << 40);
    }

    public static int getReadLockCount(final long value) {
        return (int) ((value & BITMASK_READLOCKS) >> 40);
    }

    public static long setCommitLock(final long value, final boolean commitLock) {
        return (value & ~BITMASK_COMMITLOCK) | ((commitLock ? 1L : 0L) << 63);
    }

    public static boolean hasWriteOrCommitLock(final long value) {
        return ((value & (BITMASK_COMMITLOCK + BITMASK_UPDATELOCK)) != 0);
    }

    public static boolean hasAnyLock(final long value) {
        return ((value & (BITMASK_COMMITLOCK + BITMASK_UPDATELOCK + BITMASK_READLOCKS)) != 0);
    }

    public static boolean hasCommitLock(final long value) {
        return (value & BITMASK_COMMITLOCK) != 0;
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

    private static String toOrecString(long value) {
        return format(
                "Orec(hasCommitLock=%s, hasUpdateLock=%s, readLocks=%s, surplus=%s, isReadBiased=%s, readonlyCount=%s)",
                hasCommitLock(value),
                hasWriteLock(value),
                getReadonlyCount(value),
                getSurplus(value),
                isReadBiased(value),
                getReadonlyCount(value));
    }
}

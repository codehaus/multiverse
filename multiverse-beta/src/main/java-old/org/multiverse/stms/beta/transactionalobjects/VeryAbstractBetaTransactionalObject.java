package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.Lock;
import org.multiverse.api.LockMode;
import org.multiverse.api.Transaction;
import org.multiverse.api.blocking.RetryLatch;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.Listeners;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.utils.ToolUnsafe;
import sun.misc.Unsafe;

import static java.lang.String.format;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A Orec (ownership record) that is completely safe and essentially the heart of the Multiverse-beta stm.
 * Each transactional object (e.g. a ref) has such an Orec (it extends from it to make it cheaper). It works
 * with an arrive/depart system (semi visible reads) to prevent isolation problems; when an update is done
 * on an transactional object, where the orec has a surplus of readers, you know that transactions are still
 * dependant on this ___orecValue. When this happens, the global conflict counter is increased, and all reading
 * transactions are forced to do a read conflict scan the next time they do a read (or a write since that
 * also requires a read to get the initial ___orecValue).
 * <p/>
 * Each transaction needs to track all reads (and of course all writes). To prevent contention on orecs that
 * mostly are read, an orec can become readonly after a certain number of only reads are done. Once this happens
 * additional arrives/departs are ignored. When an update happens on a readbiased orec, it will always cause
 * a conflict on the global conflict counter and even if a transaction didn't read that orec at all, it will need
 * to do a conflict scan.
 * <p/>
 * Another advantage of this approach is that transaction don't need to track all reads anymore; once something
 * has become read biased, it depends on the transaction setting trackReads if the read still is tracked. The
 * disadvantage for these transactions is that they can't recover from a change in the global conflict counter.
 * <p/>
 * Layout:
 * In total 64 bits
 * bit 0      : contains commit lock
 * bit 1      : contains update lock
 * bit 2      : contains readbiased.
 * bit 3-53   : contains surplus
 * bit 54-63  : contains readonly count
 * <p/>
 * <p/>
 * The update lock is the same as the 'ensure' and the commit lock is the same as the privatize lock.
 */
public abstract class VeryAbstractBetaTransactionalObject
        implements BetaTransactionalObject, Lock, BetaStmConstants {

    //it is important that the maximum threshold is not larger than 1023 (there are 10 bits for the readonly count)
    private static final int READBIASED_THRESHOLD = 16;

    private static final long BITMASK_COMMITLOCK = 0x8000000000000000L;
    private static final long BITMASK_UPDATELOCK = 0x4000000000000000L;
    private static final long BITMASK_READBIASED = 0x2000000000000000L;
    private static final long BITMASK_SURPLUS = 0x1FFFFFFFFFFFFE00L;
    private static final long BITMASK_READONLY_COUNT = 0x00000000000003FFL;

    protected static final Unsafe ___unsafe = ToolUnsafe.getUnsafe();
    protected static final long listenersOffset;
    protected static final long valueOffset;

    static {
        try {
            listenersOffset = ___unsafe.objectFieldOffset(
                    VeryAbstractBetaTransactionalObject.class.getDeclaredField("listeners"));
            valueOffset = ___unsafe.objectFieldOffset(
                    VeryAbstractBetaTransactionalObject.class.getDeclaredField("orec"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    public final BetaStm ___stm;

    protected volatile Listeners ___listeners;

    protected volatile long ___version;

    private volatile long ___orec;

    //This field has a controlled JMM problem (just like the hashcode of String).
    protected int ___identityHashCode;

    public VeryAbstractBetaTransactionalObject(BetaStm stm) {
        this.___stm = stm;
    }

    @Override
    public final long getVersion() {
        return ___version;
    }

    @Override
    public final BetaStm getStm() {
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
    public final int ___registerChangeListener(
            final RetryLatch latch,
            final BetaTranlocal tranlocal,
            final BetaObjectPool pool,
            final long listenerEra) {

        if (tranlocal.isCommuting() || tranlocal.isConstructing()) {
            return REGISTRATION_NONE;
        }

        final long version = tranlocal.version;

        if (version != ___version) {
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
            if (version != ___version) {
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
            if (version == ___version) {
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
    public final boolean ___tryLockAndCheckConflict(
            final BetaTransaction newLockOwner,
            final int spinCount,
            final BetaTranlocal tranlocal,
            final boolean commitLock) {

        final int currentLockMode = tranlocal.getLockMode();

        if (currentLockMode != LOCKMODE_NONE) {
            if (commitLock && currentLockMode == LOCKMODE_WRITE) {
                tranlocal.setLockMode(LOCKMODE_EXCLUSIVE);
                ___upgradeToCommitLock();
            }
            return true;
        }

        final long expectedVersion = tranlocal.version;

        //if the version already is different, we are done since we know that there is a conflict.
        if (___version != expectedVersion) {
            return false;
        }

        if (!tranlocal.hasDepartObligation()) {
            //we need to arrive as well because the the tranlocal was readbiased, and no real arrive was done.
            final int arriveStatus = ___tryLockAndArrive(spinCount, commitLock);

            if (arriveStatus == ARRIVE_LOCK_NOT_FREE) {
                return false;
            }

            if (arriveStatus == ARRIVE_NORMAL) {
                tranlocal.setDepartObligation(true);
            }
        } else if (!___tryLockAfterNormalArrive(spinCount, commitLock)) {
            return false;
        }

        //the lock was acquired successfully.
        tranlocal.setLockMode(commitLock ? LOCKMODE_EXCLUSIVE : LOCKMODE_WRITE);
        return expectedVersion == ___version;
    }

    @Override
    public final boolean ___hasReadConflict(final BetaTranlocal tranlocal) {
        if (tranlocal.getLockMode() != LOCKMODE_NONE) {
            return false;
        }

        if (___hasCommitLock()) {
            return true;
        }

        return tranlocal.version != ___version;
    }

    public final boolean isLockedForCommitByOther() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException("No transaction is found for the isPrivatizedByOther operation");
        }

        return isLockedForCommitByOther((BetaTransaction) tx);
    }

    public final boolean isLockedForCommitByOther(Transaction tx) {
        return isLockedForCommitByOther((BetaTransaction) tx);
    }

    public final boolean isLockedForCommitByOther(BetaTransaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        final BetaTranlocal tranlocal = tx.locate(this);

        if (!___hasCommitLock()) {
            return false;
        }

        return tranlocal == null || tranlocal.getLockMode() == LOCKMODE_NONE;
    }

    public final boolean isLockedForWriteBySelf(BetaTransaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        final BetaTranlocal tranlocal = tx.locate(this);
        return tranlocal != null && tranlocal.getLockMode() == LOCKMODE_WRITE;
    }

    public final boolean isLockedForWriteByOther() {
        final Transaction tx = getThreadLocalTransaction();

        if (tx == null) {
            throw new TransactionRequiredException("No transaction is found for the isEnsuredByOther operation");
        }

        return isLockedForWriteByOther((BetaTransaction) tx);
    }

    public final boolean isLockedForWriteByOther(Transaction tx) {
        return isLockedForWriteByOther((BetaTransaction) tx);
    }

    public final boolean isLockedForWriteByOther(BetaTransaction tx) {
        if (tx == null) {
            throw new NullPointerException();
        }

        final BetaTranlocal tranlocal = tx.locate(this);

        if (!___hasUpdateLock()) {
            return false;
        }

        return tranlocal == null || tranlocal.getLockMode() == LOCKMODE_NONE;
    }

    protected final int ___arriveAndLockOrBackoff() {
        for (int k = 0; k <= ___stm.defaultMaxRetries; k++) {
            final int arriveStatus = ___tryLockAndArrive(___stm.spinCount, true);
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
    public final int ___identityHashCode() {
        int tmp = ___identityHashCode;
        if (tmp != 0) {
            return tmp;
        }

        tmp = System.identityHashCode(this);
        ___identityHashCode = tmp;
        return tmp;
    }

    @Override
    public boolean ___hasLock() {
        final long current = ___orec;
        return hasUpdateLock(current) || hasCommitLock(___orec);
    }

    @Override
    public boolean ___hasUpdateLock() {
        return hasUpdateLock(___orec);
    }

    @Override
    public final boolean ___hasCommitLock() {
        return hasCommitLock(___orec);
    }

    @Override
    public final int ___getReadBiasedThreshold() {
        return READBIASED_THRESHOLD;
    }

    @Override
    public final long ___getSurplus() {
        return getSurplus(___orec);
    }

    @Override
    public final boolean ___isReadBiased() {
        return isReadBiased(___orec);
    }

    @Override
    public final int ___getReadonlyCount() {
        return getReadonlyCount(___orec);
    }

    @Override
    public final int ___arrive(int spinCount) {
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

    private void yieldIfNeeded(int remainingSpins) {
        if (remainingSpins % ___SpinYield == 0 && remainingSpins > 0) {
            Thread.yield();
        }
    }

    @Override
    public final int ___tryLockAndArrive(int spinCount, final boolean commitLock) {
        do {
            final long current = ___orec;

            if (hasLock(current)) {
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
                            "Can't arriveAndLockForUpdate; surplus is larger than 2: " + ___toOrecString(current));
                }
            } else {
                surplus++;
            }

            long next = setSurplus(current, surplus);

            if (commitLock) {
                next = setCommitLock(next, true);
            } else {
                next = setUpdateLock(next, true);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_UNREGISTERED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }

    @Override
    public final boolean ___tryLockAfterNormalArrive(int spinCount, final boolean commitLock) {
        do {
            final long current = ___orec;

            if (hasLock(current)) {
                spinCount--;
                yieldIfNeeded(spinCount);
                continue;
            }

            if (getSurplus(current) == 0) {
                throw new PanicError(
                        "Can't acquire the updatelock is there is no surplus (so if it didn't do a read before)" +
                                ___toOrecString(current));
            }


            long next = current;
            if (commitLock) {
                next = setCommitLock(next, true);
            } else {
                next = setUpdateLock(current, true);
            }

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return true;
            }
        } while (spinCount >= 0);

        return false;
    }

    @Override
    public void ___upgradeToCommitLock() {
        while (true) {
            final long current = ___orec;

            if (hasCommitLock(current)) {
                return;
            }

            if (!hasUpdateLock(current)) {
                throw new PanicError("Can't upgradeToCommitLock is the updateLock is not acquired");
            }

            long next = setCommitLock(current, true);
            next = setUpdateLock(next, false);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    @Override
    public final void ___departAfterReading() {
        while (true) {
            final long current = ___orec;
            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError("Can't depart if there is no surplus " + ___toOrecString(current));
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError("Can't depart from a readbiased orec " + ___toOrecString(current));
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

    @Override
    public final void ___departAfterReadingAndUnlock() {
        while (true) {
            final long current = ___orec;
            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError(
                        "Can't ___departAfterReadingAndUnlock if there is no surplus: " + ___toOrecString(current));
            }

            if (!hasLock(current)) {
                throw new PanicError(
                        "Can't ___departAfterReadingAndUnlock if the lock is not acquired " + ___toOrecString(current));
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError(
                        "Can't ___departAfterReadingAndUnlock when readbiased orec " + ___toOrecString(current));
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

            long next = setCommitLock(current, false);
            next = setUpdateLock(next, false);
            next = setIsReadBiased(next, isReadBiased);
            next = setReadonlyCount(next, readonlyCount);
            next = setSurplus(next, surplus);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    @Override
    public final long ___departAfterUpdateAndUnlock() {
        boolean conflictSend = false;
        while (true) {
            final long current = ___orec;

            if (!hasCommitLock(current)) {
                throw new PanicError(
                        "Can't ___departAfterUpdateAndUnlock is the commit lock is not acquired " + ___toOrecString(current));
            }

            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError(
                        "Can't ___departAfterUpdateAndUnlock is there is no surplus " + ___toOrecString(current));
            }

            boolean conflict;
            if (isReadBiased(current)) {
                if (surplus > 1) {
                    throw new PanicError(
                            "The surplus can never be larger than 1 if readBiased " + ___toOrecString(current));
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

    @Override
    public final long ___departAfterFailureAndUnlock() {
        while (true) {
            final long current = ___orec;

            if (!hasLock(current)) {
                throw new PanicError(
                        "Can't ___departAfterFailureAndUnlock if the lock was not acquired " + ___toOrecString(current));
            }

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError(
                        "Can't ___departAfterFailureAndUnlock if there is no surplus " + ___toOrecString(current));
            }

            //we can only decrease the surplus if it is not read biased. Because with a read biased
            //orec, we have no idea how many readers there are.
            if (isReadBiased(current)) {
                if (surplus > 1) {
                    throw new PanicError(
                            "Can't ___departAfterFailureAndUnlock with a surplus larger than 1 if " +
                                    "the orec is read biased " + ___toOrecString(current));
                }
            } else {
                surplus--;
            }

            long next = setCommitLock(current, false);
            next = setUpdateLock(next, false);
            next = setSurplus(next, surplus);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return surplus;
            }
        }
    }


    @Override
    public final void ___departAfterFailure() {
        while (true) {
            final long current = ___orec;

            if (isReadBiased(current)) {
                throw new PanicError("Can't departAfterFailure when orec is readbiased:" + ___toOrecString(current));
            }

            long surplus = getSurplus(current);

            if (hasCommitLock(current)) {
                if (surplus < 2) {
                    throw new PanicError(
                            "there must be at least 2 readers, the thread that acquired the lock, " +
                                    "and the calling thread " + ___toOrecString(current));
                }
            } else {
                if (surplus == 0) {
                    throw new PanicError(
                            "Can't departAfterFailure if there is no surplus " + ___toOrecString(current));
                }
            }
            surplus--;

            long next = setSurplus(current, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    @Override
    public final void ___unlockByReadBiased() {
        while (true) {
            final long current = ___orec;

            if (!isReadBiased(current)) {
                throw new PanicError(
                        "Can't ___unlockByReadBiased when it is not readbiased " + ___toOrecString(current));
            }

            if (!hasLock(current)) {
                throw new PanicError(
                        "Can't ___unlockByReadBiased if it isn't locked " + ___toOrecString(current));
            }

            if (getSurplus(current) > 1) {
                throw new PanicError(
                        "Surplus for a readbiased orec never can be larger than 1 " + ___toOrecString(current));
            }

            long next = setCommitLock(current, false);
            next = setUpdateLock(next, false);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public final String ___toOrecString() {
        return ___toOrecString(___orec);
    }

    public static long setCommitLock(final long value, final boolean commitLock) {
        return (value & ~BITMASK_COMMITLOCK) | ((commitLock ? 1L : 0L) << 63);
    }

    public static boolean hasLock(final long value) {
        return ((value & (BITMASK_COMMITLOCK + BITMASK_UPDATELOCK)) != 0);
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

    public static boolean hasUpdateLock(final long value) {
        return (value & BITMASK_UPDATELOCK) != 0;
    }

    public static long setUpdateLock(final long value, final boolean updateLock) {
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

    private static String ___toOrecString(long value) {
        return format(
                "Orec(hasCommitLock=%s, hasUpdateLock=%s, surplus=%s, isReadBiased=%s, readonlyCount=%s)",
                hasCommitLock(value),
                hasUpdateLock(value),
                getSurplus(value),
                isReadBiased(value),
                getReadonlyCount(value));
    }

    @Override
    public void acquire(LockMode desiredLockMode) {
        throw new TodoException();
    }

    @Override
    public void acquire(Transaction tx, LockMode desiredLockMode) {
        throw new TodoException();
    }

    @Override
    public LockMode atomicGetLockMode() {
        throw new UnsupportedOperationException();
    }

    public LockMode getLockMode() {
        throw new TodoException();
    }

    public LockMode getLockMode(Transaction tx) {
        throw new TodoException();
    }

    @Override
    public boolean tryAcquire(LockMode desiredLockMode) {
        throw new TodoException();
    }

    @Override
    public boolean tryAcquire(Transaction tx, LockMode desiredLockMode) {
        throw new TodoException();
    }
}

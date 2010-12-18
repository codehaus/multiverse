package org.multiverse.stms.beta.orec;

import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.utils.ToolUnsafe;
import sun.misc.Unsafe;

import static java.lang.String.format;

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
 *
 * @author Peter Veentjer
 */
public class FastOrec implements Orec {

    //it is important that the maximum threshold is not larger than 1023 (there are 10 bits for the readonly count)
    private static final int READBIASED_THRESHOLD = 16;
    private static final long BITMASK_COMMITLOCK = 0x8000000000000000L;
    private static final long BITMASK_UPDATELOCK = 0x4000000000000000L;
    private static final long BITMASK_READBIASED = 0x2000000000000000L;
    private static final long BITMASK_SURPLUS = 0x1FFFFFFFFFFFFE00L;
    private static final long BITMASK_READONLY_COUNT = 0x00000000000003FFL;

    protected static final Unsafe ___unsafe = ToolUnsafe.getUnsafe();
    protected static final long valueOffset;

    static {
        try {
            valueOffset = ___unsafe.objectFieldOffset(FastOrec.class.getDeclaredField("___orecValue"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private volatile long ___orecValue;

    @Override
    public boolean ___hasLock() {
        final long current = ___orecValue;
        return hasUpdateLock(current) || hasCommitLock(___orecValue);
    }

    @Override
    public boolean ___hasUpdateLock() {
        return hasUpdateLock(___orecValue);
    }

    @Override
    public final boolean ___hasCommitLock() {
        return hasCommitLock(___orecValue);
    }

    @Override
    public final int ___getReadBiasedThreshold() {
        return READBIASED_THRESHOLD;
    }

    @Override
    public final long ___getSurplus() {
        return getSurplus(___orecValue);
    }

    @Override
    public final boolean ___isReadBiased() {
        return isReadBiased(___orecValue);
    }

    @Override
    public final int ___getReadonlyCount() {
        return getReadonlyCount(___orecValue);
    }

    @Override
    public final int ___arrive(int spinCount) {
        do {
            final long current = ___orecValue;

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
            final long current = ___orecValue;

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
            final long current = ___orecValue;

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
            final long current = ___orecValue;

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
            final long current = ___orecValue;
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
            final long current = ___orecValue;
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
    public final long ___departAfterUpdateAndUnlock(
            final GlobalConflictCounter globalConflictCounter, final BetaTransactionalObject transactionalObject) {

        boolean conflictSend = false;
        while (true) {
            final long current = ___orecValue;

            if (!hasLock(current)) {
                throw new PanicError(
                        "Can't ___departAfterUpdateAndUnlock is the update/commit lock is not acquired " + ___toOrecString(current));
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

            if (conflict) {
                if (!conflictSend) {
                    globalConflictCounter.signalConflict(transactionalObject);
                    conflictSend = true;
                }
            }

            if (surplus == 0 && hasCommitLock(current)) {
                ___orecValue = 0;
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
            final long current = ___orecValue;

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
            final long current = ___orecValue;

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
            final long current = ___orecValue;

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
        return ___toOrecString(___orecValue);
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
                "FastOrec(hasCommitLock=%s, hasUpdateLock=%s, surplus=%s, isReadBiased=%s, readonlyCount=%s)",
                hasCommitLock(value),
                hasUpdateLock(value),
                getSurplus(value),
                isReadBiased(value),
                getReadonlyCount(value));
    }

}
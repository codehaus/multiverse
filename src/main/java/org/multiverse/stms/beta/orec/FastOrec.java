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
 * dependant on this value. When this happens, the global conflict counter is increased, and all reading
 * transactions are forced to do a read conflict scan the next time they do a read (or a write since that
 * also requires a read to get the initial value).
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
 * 0: bit contains lock
 * 1: bit contains readbiased.
 * 3-54: contains surplus
 * 54-63 contains readonly count
 * <p/>
 * <p/>
 * Cause of problems perhaps; while it is locked, only 'simple' departs should be allowed. But it should not
 * be allowed that the readbiased behavior is changed. Atm it can happen. So when a transaction has locked an orec,
 * and an other one does a depart
 *
 * @author Peter Veentjer
 */
public class FastOrec implements Orec {

    protected final static Unsafe ___unsafe = ToolUnsafe.getUnsafe();

    protected final static long valueOffset;

    static {
        try {
            valueOffset = ___unsafe.objectFieldOffset(FastOrec.class.getDeclaredField("value"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    //it is important that the maximum threshold is not larger than 1023 (there are 10 bits for
    //the readonly count)
    public final static int ___READBIASED_THRESHOLD = 16;

    private volatile long value;

    @Override
    public final int ___getReadBiasedThreshold() {
        return ___READBIASED_THRESHOLD;
    }

    @Override
    public final long ___getSurplus() {
        return getSurplus(value);
    }

    @Override
    public final boolean ___isReadBiased() {
        return isReadBiased(value);
    }

    @Override
    public final boolean ___isLocked() {
        return isLocked(value);
    }

    @Override
    public final int ___getReadonlyCount() {
        return getReadonlyCount(value);
    }

    @Override
    public final boolean ___query() {
        return getSurplus(value) > 0;
    }

    public volatile boolean requiresOnUpdateBiased = false;

    @Override
    public final boolean ___arrive(int spinCount) {
        do {
            long current = value;

            if (isLocked(current)) {
                spinCount--;
                continue;
            }

            long surplus = getSurplus(current);

            if (isReadBiased(current)) {
                if (surplus == 0) {
                    surplus = 1;
                } else if (surplus == 1) {
                    return true;
                } else {
                    throw new PanicError("Surplus for a readbiased orec can never be larger than 1");
                }
            } else {
                surplus++;
            }

            long next = setSurplus(current, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return true;
            }
        } while (spinCount >= 0);

        return false;
    }

    @Override
    public final int ___arrive2(int spinCount) {
        do {
            long current = value;

            if (isLocked(current)) {
                spinCount--;
                continue;
            }

            long surplus = getSurplus(current);

            final boolean isReadBiased = isReadBiased(current);

            if (isReadBiased) {
                if (surplus == 0) {
                    surplus = 1;
                } else if (surplus == 1) {
                    return ARRIVE_READBIASED;
                } else {
                    throw new PanicError("Surplus for a readbiased orec can never be larger than 1");
                }
            } else {
                surplus++;
            }

            long next = setSurplus(current, surplus);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_READBIASED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }

    @Override
    public final boolean ___arriveAndLock(int spinCount) {
        do {
            long current = value;

            if (isLocked(current)) {
                spinCount--;
                continue;
            }

            long surplus = getSurplus(current);
            if (isReadBiased(current)) {
                if (surplus == 0) {
                    surplus = 1;
                } else if (surplus > 1) {
                    throw new PanicError("Can't arriveAndLockForUpdate; surplus is larger than 2: " + ___toOrecString(current));
                }
            } else {
                surplus++;
            }

            long next = setSurplus(current, surplus);
            next = setLocked(next, true);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return true;
            }
        } while (spinCount >= 0);

        return false;
    }

    @Override
    public final int ___arriveAndLock2(int spinCount) {
        do {
            long current = value;

            if (isLocked(current)) {
                spinCount--;
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
            next = setLocked(next, true);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return isReadBiased ? ARRIVE_READBIASED : ARRIVE_NORMAL;
            }
        } while (spinCount >= 0);

        return ARRIVE_LOCK_NOT_FREE;
    }


    @Override
    public final void ___departAfterReading() {
        while (true) {
            long current = value;
            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError("Can't depart if there is no surplus " + ___toOrecString(current));
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError("Can't depart from a readbiased orec " + ___toOrecString(current));
            }

            int readonlyCount = getReadonlyCount(current);
            if (readonlyCount < ___READBIASED_THRESHOLD) {
                readonlyCount++;
            }

            surplus--;
            final boolean isLocked = isLocked(current);
            if (!isLocked && surplus == 0 && readonlyCount == ___READBIASED_THRESHOLD) {
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
    public final void ___departAfterReadingAndReleaseLock() {
        while (true) {
            long current = value;
            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError(
                        "Can't departAfterReadingAndReleaseLock if there is no surplus: " + ___toOrecString(current));
            }

            final boolean isLocked = isLocked(current);
            if (!isLocked) {
                throw new PanicError(
                        "Can't departAfterReadingAndReleaseLock if the lock is not acquired " + ___toOrecString(current));
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError(
                        "Can't departAfterReadingAndReleaseLock when readbiased orec " + ___toOrecString(current));
            }

            int readonlyCount = getReadonlyCount(current);

            surplus--;

            if (readonlyCount < ___READBIASED_THRESHOLD) {
                readonlyCount++;
            }

            if (surplus == 0 && readonlyCount == ___READBIASED_THRESHOLD) {
                isReadBiased = true;
                readonlyCount = 0;
            }

            long next = setLocked(current, false);
            next = setIsReadBiased(next, isReadBiased);
            next = setReadonlyCount(next, readonlyCount);
            next = setSurplus(next, surplus);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    @Override
    public final long ___departAfterUpdateAndReleaseLock(
            final GlobalConflictCounter globalConflictCounter, final BetaTransactionalObject transactionalObject) {

        while (true) {
            long current = value;

            if (!isLocked(current)) {
                throw new PanicError(
                        "Can't departAfterUpdateAndReleaseLock is the lock is not acquired " + ___toOrecString(current));
            }

            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError(
                        "Can't departAfterUpdateAndReleaseLock is there is no surplus " + ___toOrecString(current));
            }

            boolean conflict;
            if (isReadBiased(current)) {
                if (surplus > 1) {
                    throw new PanicError(
                            "The surplus can never be larger than 1 if readBiased " + ___toOrecString(current));
                }

                //System.out.println(Thread.currentThread() + " update after readbiased");

                //there always is a conflict when a readbiased orec is updated.
                conflict = true;
                surplus = 0;
            } else {
                surplus--;
                conflict = surplus > 0;
            }

            if (conflict) {
                globalConflictCounter.signalConflict(transactionalObject);
            }

            long next = setLocked(current, false);
            next = setReadonlyCount(next, 0);
            next = setIsReadBiased(next, false);
            next = setSurplus(next, surplus);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return surplus;
            }
        }
    }

    @Override
    public final long ___departAfterFailureAndReleaseLock() {
        while (true) {
            long current = value;

            if (!isLocked(current)) {
                throw new PanicError(
                        "Can't departAfterFailureAndReleaseLock if the lock was not acquired " + ___toOrecString(current));
            }

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError(
                        "Can't departAfterFailureAndReleaseLock if there is no surplus " + ___toOrecString(current));
            }

            //we can only decrease the surplus if it is not read biased. Because with a read biased
            //orec, we have no idea how many readers there are.
            if (isReadBiased(current)) {
                if (surplus > 1) {
                    throw new PanicError(
                            "Can't departAfterFailureAndReleaseLock with a surplus larger than 1 if " +
                                    "the orec is read biased " + ___toOrecString(current));
                }
            } else {
                surplus--;
            }

            long next = setLocked(current, false);
            next = setSurplus(next, surplus);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return surplus;
            }
        }
    }

    @Override
    public final void ___departAfterFailure() {
        while (true) {
            long current = value;

            if (isReadBiased(current)) {
                throw new PanicError("Can't departAfterFailure when orec is readbiased:" + ___toOrecString(current));
            }

            long surplus = getSurplus(current);

            if (isLocked(current)) {
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
    public final boolean ___tryLockAfterArrive(int spinCount) {
        do {
            long current = value;

            if (isLocked(current)) {
                spinCount--;
                continue;
            }

            if (___getSurplus() == 0) {
                throw new PanicError(
                        "Can't acquire the updatelock is there is no surplus (so if it didn't do a read before)" +
                                ___toOrecString(current));
            }

            long next = setLocked(current, true);

            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return true;
            }
        } while (spinCount >= 0);

        return false;
    }

//    @Override

    public final void ___releaseLockByReadBiased() {
        while (true) {
            long current = value;

            if (!isReadBiased(current)) {
                throw new PanicError(
                        "Can't releaseLockByReadBiased when it is not readbiased " + ___toOrecString(current));
            }

            if (!isLocked(current)) {
                throw new PanicError(
                        "Can't releaseLockByReadBiased if it isn't locked " + ___toOrecString(current));
            }

            if (getSurplus(current) > 1) {
                throw new PanicError(
                        "Surplus for a readbiased orec never can be larger than 1 " + ___toOrecString(current));
            }

            long next = setLocked(current, false);
            if (___unsafe.compareAndSwapLong(this, valueOffset, current, next)) {
                return;
            }
        }
    }

    public static long setLocked(final long value, final boolean isLocked) {
        return (value & ~0x8000000000000000L) | ((isLocked ? 1L : 0L) << 63);
    }

    public static boolean isLocked(final long value) {
        return (value & 0x8000000000000000L) != 0;
    }

    public static long setIsReadBiased(final long value, final boolean isReadBiased) {
        return (value & ~0x4000000000000000L) | ((isReadBiased ? 1L : 0L) << 62);
    }

    public static boolean isReadBiased(final long value) {
        return (value & 0x4000000000000000L) != 0;
    }

    public static int getReadonlyCount(final long value) {
        return (int) (value & 0x00000000000003FFL);
    }

    public static long setReadonlyCount(final long value, final int readonlyCount) {
        return (value & ~0x00000000000003FFL) | readonlyCount;
    }

    public static long setSurplus(final long value, final long surplus) {
        return (value & ~0x3FFFFFFFFFFFFC00L) | (surplus << 10);
    }

    public static long getSurplus(final long value) {
        return (value & 0x3FFFFFFFFFFFFC00L) >> 10;
    }

    private static String ___toOrecString(long value) {
        return format("FastOrec(isLocked=%s, surplus=%s, isReadBiased=%s, readonlyCount=%s)",
                isLocked(value), getSurplus(value), isReadBiased(value), getReadonlyCount(value));
    }

    public final String ___toOrecString() {
        return ___toOrecString(value);
    }
}

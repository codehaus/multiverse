package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.Orec;
import org.multiverse.stms.beta.orec.ToolUnsafe;
import sun.misc.Unsafe;

import static java.lang.String.format;

/**
 * Orec implementation.
 *
 * @author Peter Veentjer.
 */
public abstract class FastOrec implements Orec {

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
    public final static int ___READ_THRESHOLD = 16;

    private volatile long value;

    /**
     * Atomically sets the value to the given updated value
     * if the current value {@code ==} the expected value.
     *
     * @param expect the expected value
     * @param update the new value
     * @return true if successful. False return indicates that
     *         the actual value was not equal to the expected value.
     */
    protected boolean ___compareAndSet(final long expect, final long update) {
        return ___unsafe.compareAndSwapLong(this, valueOffset, expect, update);
    }

    @Override
    public final int ___getReadBiasedThreshold() {
        return ___READ_THRESHOLD;
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

    @Override
    public final boolean ___arrive(int spinCount) {
        do {
            long current = value;

            if (isLocked(current)) {
                spinCount--;
            } else {
                long surplus = getSurplus(current);

                if (isReadBiased(current)) {
                    if (surplus > 0) {
                        return true;
                    }

                    surplus = 1;
                } else {
                    surplus++;
                }

                long next = setSurplus(current, surplus);

                if (___compareAndSet(current, next)) {
                    return true;
                }
            }
        } while (spinCount >= 0);

        return false;
    }

    @Override
    public final boolean ___arriveAndLockForUpdate(int spinCount) {
        do {
            long current = value;

            if (isLocked(current)) {
                spinCount--;
            } else {
                long surplus = getSurplus(current);

                surplus = isReadBiased(current) ? 1 : surplus + 1;

                long next = setSurplus(current, surplus);
                next = setLocked(next, true);

                if (___compareAndSet(current, next)) {
                    return true;
                }
            }
        } while (spinCount >= 0);

        return false;
    }

    @Override
    public final boolean ___departAfterReading() {
        while (true) {
            long current = value;
            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError();
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError();
            }

            int readonlyCount = getReadonlyCount(current);

            surplus--;
            readonlyCount++;
            boolean isLocked = isLocked(current);
            if (surplus == 0 && readonlyCount >= ___READ_THRESHOLD) {
                isReadBiased = true;
                isLocked = true;
                readonlyCount = 0;
            }

            long next = setLocked(current, isLocked);
            next = setIsReadBiased(next, isReadBiased);
            next = setReadonlyCount(next, readonlyCount);
            next = setSurplus(next, surplus);
            if (___compareAndSet(current, next)) {
                return isReadBiased;
            }
        }
    }

    @Override
    public final boolean ___departAfterReadingAndReleaseLock() {
        while (true) {
            long current = value;
            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError();
            }

            boolean isLocked = isLocked(current);
            if (!isLocked) {
                throw new PanicError("Can't departAfterReadingAndReleaseLock if the lock is not acquired");
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError();
            }

            int readonlyCount = getReadonlyCount(current);

            surplus--;
            readonlyCount++;
            if (surplus == 0 && readonlyCount >= ___READ_THRESHOLD) {
                isReadBiased = true;
                isLocked = true;
                readonlyCount = 0;
            } else {
                isLocked = false;
            }

            long next = setLocked(current, isLocked);
            next = setIsReadBiased(next, isReadBiased);
            next = setReadonlyCount(next, readonlyCount);
            next = setSurplus(next, surplus);
            if (___compareAndSet(current, next)) {
                return isReadBiased;
            }
        }
    }

    @Override
    public final long ___departAfterUpdateAndReleaseLock(
            final GlobalConflictCounter globalConflictCounter, final BetaTransactionalObject transactionalObject) {
        while (true) {
            long current = value;

            if (!isLocked(current)) {
                throw new PanicError();
            }

            boolean isReadBiased = isReadBiased(current);
            long surplus = getSurplus(current);

            boolean conflict;
            long resultingSurplus;
            if (isReadBiased) {
                conflict = surplus > 0;
                //todo: correct?
                resultingSurplus = surplus;
                surplus = 0;
                isReadBiased = false;
            } else {
                surplus--;
                conflict = surplus > 0;
                resultingSurplus = surplus;
            }

            if (conflict) {
                globalConflictCounter.signalConflict(transactionalObject);
            }

            long next = setLocked(current, false);
            next = setReadonlyCount(next, 0);
            next = setIsReadBiased(next, isReadBiased);
            next = setSurplus(next, surplus);
            if (___compareAndSet(current, next)) {
                return resultingSurplus;
            }
        }
    }

    @Override
    public final long ___departAfterFailureAndReleaseLock() {
        while (true) {
            long current = value;

            if (!isLocked(current)) {
                throw new PanicError();
            }

            long surplus = getSurplus(current);
            if (surplus == 0) {
                throw new PanicError();
            }

            //we can only decrease the surplus if it is not read biased. Because with a read biased
            //orec, we have no idea how many readers there are.
            if (!isReadBiased(current)) {
                surplus--;
            }

            long next = setLocked(current, false);
            next = setSurplus(next, surplus);
            if (___compareAndSet(current, next)) {
                return surplus;
            }
        }
    }

    @Override
    public final void ___departAfterFailure() {
        while (true) {
            long current = value;

            if (isReadBiased(current)) {
                throw new PanicError();
            }

            long surplus = getSurplus(current);

            if (isLocked(current)) {
                if (surplus < 2) {
                    throw new PanicError();
                }
            } else {
                if (surplus == 0) {
                    throw new PanicError();
                }
            }
            surplus--;

            long next = setSurplus(current, surplus);

            if (___compareAndSet(current, next)) {
                return;
            }
        }
    }

    @Override
    public final boolean ___tryUpdateLock(int spinCount) {
        do {
            long current = value;

            if (isLocked(current)) {
                spinCount--;
            } else {
                if (___getSurplus() == 0) {
                    throw new PanicError();
                }

                long next = setLocked(current, true);

                if (___compareAndSet(current, next)) {
                    return true;
                }
            }
        } while (spinCount >= 0);

        return false;
    }

//    @Override

    public final void ___unlockByPermanent() {
        while (true) {
            long current = value;

            if (!isLocked(current)) {
                throw new PanicError();
            }

            long next = setLocked(current, false);
            if (___compareAndSet(current, next)) {
                return;
            }
        }
    }

    @Override
    public final void ___unlockAfterBecomingReadBiased() {
        while (true) {
            long current = value;

            if (!isLocked(current)) {
                throw new PanicError();
            }

            long next = setLocked(current, false);
            if (___compareAndSet(current, next)) {
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

    public final String toOrecString() {
        return format("FastOrec(isLocked=%s, surplus=%s, ___isReadBiased=%s, readonlyCount=%s)",
                isLocked(value), getSurplus(value), isReadBiased(value), getReadonlyCount(value));
    }
}
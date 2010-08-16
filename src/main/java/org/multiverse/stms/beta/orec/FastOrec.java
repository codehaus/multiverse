package org.multiverse.stms.beta.orec;

import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

import java.util.concurrent.atomic.AtomicLong;

import static java.lang.String.format;

/**
 * A Orec (ownership record) that is completely safe and essentially the heart of the Multiverse-beta stm.
 * Each transactional object (e.g. a ref) has such an Orec (it extends from it to make it cheaper). It works
 * with an arrive/depart system (semi visible reads) to prevent isolation problems; when an update is done
 * on an transactional object, where the orec has a surplus of readers, you know that transactions are still
 * dependant on this value. When this happens, the global conflict counter is increased, and all reading
 * transactions are forced to do a read conflict scan the next time they do a read (or a write since that
 * also requires a read to get the initial value).
 *
 * Layout:
 * In total 64 bits
 * 0: bit contains lock
 * 1: bit contains readbiased.
 * 3-54: contains surplus
 * 54-63 contains readonly count
 *
 * @author Peter Veentjer
 */
public final class FastOrec implements Orec {

    //it is important that the maximum threshold is not larger than 1023 (there are 10 bits for
    //the readonly count)
    public final static int READ_THRESHOLD = 3;

    private final AtomicLong state = new AtomicLong(0);

    @Override
    public int getReadBiasedThreshold() {
        return READ_THRESHOLD;
    }

    @Override
    public long getSurplus() {
        return getSurplus(state.get());
    }

    @Override
    public boolean arrive(int spinCount) {
        do {
            long current = state.get();

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

                if (state.compareAndSet(current, next)) {
                    return true;
                }
            }
        } while (spinCount >= 0);

        return false;
    }

    @Override
    public boolean arriveAndLockForUpdate(int spinCount) {
        do {
            long current = state.get();

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
                next = setLocked(next, true);

                if (state.compareAndSet(current, next)) {
                    return true;
                }
            }
        } while (spinCount >= 0);

        return false;
    }

    @Override
    public boolean query() {
        return getSurplus(state.get()) > 0;
    }

    @Override
    public boolean departAfterReading() {
        while (true) {
            long current = state.get();
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
            if (surplus == 0 && readonlyCount >= READ_THRESHOLD) {
                isReadBiased = true;
                isLocked = true;
                readonlyCount = 0;
            }

            long next = setLocked(current, isLocked);
            next = setIsReadBiased(next, isReadBiased);
            next = setReadonlyCount(next, readonlyCount);
            next = setSurplus(next, surplus);
            if (state.compareAndSet(current, next)) {
                return isReadBiased;
            }
        }
    }

    @Override
    public int getReadonlyCount() {
        return getReadonlyCount(state.get());
    }

    @Override
    public final boolean departAfterReadingAndReleaseLock() {
        while (true) {
            long current = state.get();
            long surplus = getSurplus(current);

            if (surplus == 0) {
                throw new PanicError();
            }

            boolean isLocked = isLocked(current);
            if (!isLocked) {
                throw new PanicError();
            }

            boolean isReadBiased = isReadBiased(current);
            if (isReadBiased) {
                throw new PanicError();
            }

            int readonlyCount = getReadonlyCount(current);

            surplus--;
            readonlyCount++;
            if (surplus == 0 && readonlyCount >= READ_THRESHOLD) {
                isReadBiased = true;
                isLocked = true;
                readonlyCount = 0;
            }

            long next = setLocked(current, isLocked);
            next = setIsReadBiased(next, isReadBiased);
            next = setReadonlyCount(next, readonlyCount);
            next = setSurplus(next, surplus);
            if (state.compareAndSet(current, next)) {
                return isReadBiased;
            }
        }
    }

    @Override
    public long departAfterUpdateAndReleaseLock(GlobalConflictCounter globalConflictCounter, BetaTransactionalObject ref) {
        while (true) {
            long current = state.get();

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
                globalConflictCounter.signalConflict(ref);
            }

            long next = setLocked(current, false);
            next = setReadonlyCount(next, 0);
            next = setIsReadBiased(next, isReadBiased);
            next = setSurplus(next, surplus);
            if (state.compareAndSet(current, next)) {
                return resultingSurplus;
            }
        }
    }

    @Override
    public long departAfterFailureAndReleaseLock() {
        while (true) {
            long current = state.get();

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
            if (state.compareAndSet(current, next)) {
                return surplus;
            }
        }
    }

    @Override
    public void departAfterFailure() {
        while (true) {
            long current = state.get();

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
            if (state.compareAndSet(current, next)) {
                return;
            }
        }
    }

    @Override
    public boolean isReadBiased() {
        return isReadBiased(state.get());
    }

    @Override
    public boolean isLocked() {
        return isLocked(state.get());
    }

    @Override
    public boolean tryUpdateLock(int spinCount) {
        do {
            long current = state.get();

            if (isLocked(current)) {
                spinCount--;
            } else {
                if (getSurplus() == 0) {
                    throw new PanicError();
                }

                long next = setLocked(current, true);

                if (state.compareAndSet(current, next)) {
                    return true;
                }
            }
        } while (spinCount >= 0);

        return false;
    }

    @Override
    public void unlockAfterBecomingReadBiased() {
        while (true) {
            long current = state.get();

            if (!isLocked(current)) {
                throw new PanicError();
            }

            long next = setLocked(current, false);
            if (state.compareAndSet(current, next)) {
                return;
            }
        }
    }

    @Override
    public String toString() {
        long value = state.get();
        return format("FastOrec(isLocked=%s, surplus=%s, isReadBiased=%s, readonlyCount=%s)",
                isLocked(value), getSurplus(value), isReadBiased(value), getReadonlyCount(value));
    }

    private static long setLocked(long value, boolean isLocked) {
        return (value & ~0x8000000000000000L) | ((isLocked ? 1L : 0L) << 63);
    }

    private static boolean isLocked(long value) {
        return (value & 0x8000000000000000L) != 0;
    }

    private static long setIsReadBiased(long value, boolean isReadBiased) {
        return (value & ~0x4000000000000000L) | ((isReadBiased ? 1L : 0L) << 62);
    }

    private static boolean isReadBiased(long value) {
        return (value & 0x4000000000000000L) != 0;
    }

    private static int getReadonlyCount(long value) {
        return (int) (value & 0x00000000000003FFL);
    }

    private static long setReadonlyCount(long value, int readonlyCount) {
        return (value & ~0x00000000000003FFL) | readonlyCount;
    }

    private static long setSurplus(long value, long surplus) {
        return (value & ~0x3FFFFFFFFFFFFC00L) | (surplus << 10);
    }

    private static long getSurplus(long value) {
        return (value & 0x3FFFFFFFFFFFFC00L) >> 10;
    }
}

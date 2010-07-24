package org.multiverse.stms.beta.orec;

import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.api.exceptions.TodoException;

/**
 * A thread unsafe snzi, useful for figuring out the amount of overhead caused by synchronization
 * actions or more expensive snzi structures. it also is used to figure out the behavior needed.
 * Eventually it will be placed in the LowContentionOrec and all state will be encoded in a single
 * long that atomically is updated.
 *
 * @author Peter Veentjer
 */
public final class UnsafeOrec implements Orec {

    public final static int READ_THRESHOLD = 3;

    private int surplus = 0;
    private int readonlyCount = 0;
    private boolean isLocked = false;
    private boolean isReadBiased = false;

    @Override
    public int getReadBiasedThreshold() {
        return READ_THRESHOLD;
    }

    @Override
    public long getSurplus() {
        return surplus;
    }

    @Override
    public boolean arrive(int spinCount) {
        if (isLocked) {
            return false;
        }

        if (isReadBiased) {
            if (surplus == 0) {
                surplus = 1;
            }
        } else {
            surplus++;
        }

        return true;
    }

    @Override
    public boolean query() {
        return surplus > 0;
    }

    @Override
    public boolean departAfterReading() {
        if (surplus == 0) {
            throw new IllegalStateException();
        }

        if (isReadBiased) {
            throw new IllegalStateException();
        }

        surplus--;
        readonlyCount++;
        if (surplus == 0 && readonlyCount >= READ_THRESHOLD) {
            isReadBiased = true;
            isLocked = true;
        }


        return isReadBiased;
    }

    @Override
    public int getReadonlyCount() {
        return readonlyCount;
    }

    @Override
    public long departAfterUpdateAndReleaseLock(GlobalConflictCounter globalConflictCounter, BetaTransactionalObject ref) {
        if (!isLocked) {
            throw new IllegalStateException();
        }

        boolean conflict;
        int resultingSurplus;
        if (isReadBiased) {
            conflict = surplus > 0;
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

        readonlyCount = 0;
        isLocked = false;
        return resultingSurplus;
    }

    @Override
    public boolean departAfterReadingAndReleaseLock() {
        throw new TodoException();
    }

    @Override
    public long departAfterFailureAndReleaseLock() {
        if (!isLocked) {
            throw new IllegalStateException();
        }

        surplus--;
        isLocked = false;
        return surplus;
    }

    @Override
    public void departAfterFailure() {
        if (isReadBiased) {
            throw new IllegalStateException();
        }

        if (isLocked) {
            if (surplus < 2) {
                throw new IllegalStateException();
            }
        } else {
            if (surplus == 0) {
                throw new IllegalStateException();
            }
        }

        surplus--;
    }

    @Override
    public boolean isReadBiased() {
        return isReadBiased;
    }

    @Override
    public boolean isLocked() {
        return isLocked;
    }

    @Override
    public boolean tryUpdateLock(int spinCount) {
        if (isLocked) {
            return false;
        }

        if (surplus == 0) {
            throw new IllegalStateException();
        }

        isLocked = true;
        return true;
    }

    @Override
    public void unlockAfterBecomingReadBiased() {
        if (!isLocked) {
            throw new IllegalStateException();
        }

        isLocked = false;
    }

    @Override
    public boolean arriveAndLockForUpdate(int spinCount) {
        throw new TodoException();
    }
}

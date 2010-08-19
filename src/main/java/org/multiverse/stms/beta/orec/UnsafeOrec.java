package org.multiverse.stms.beta.orec;

import org.multiverse.api.exceptions.PanicError;
import org.multiverse.api.exceptions.TodoException;
import org.multiverse.stms.beta.BetaTransactionalObject;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;

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
    public int ___getReadBiasedThreshold() {
        return READ_THRESHOLD;
    }

    @Override
    public long ___getSurplus() {
        return surplus;
    }

    @Override
    public boolean ___arrive(int spinCount) {
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
    public boolean ___query() {
        return surplus > 0;
    }

    @Override
    public boolean ___departAfterReading() {
        if (surplus == 0) {
            throw new PanicError();
        }

        if (isReadBiased) {
            throw new PanicError();
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
    public int ___getReadonlyCount() {
        return readonlyCount;
    }

    @Override
    public long ___departAfterUpdateAndReleaseLock(GlobalConflictCounter globalConflictCounter, BetaTransactionalObject ref) {
        if (!isLocked) {
            throw new PanicError();
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
    public boolean ___departAfterReadingAndReleaseLock() {
        throw new TodoException();
    }

    @Override
    public long ___departAfterFailureAndReleaseLock() {
        if (!isLocked) {
            throw new PanicError();
        }

        surplus--;
        isLocked = false;
        return surplus;
    }

    @Override
    public void ___departAfterFailure() {
        if (isReadBiased) {
            throw new PanicError();
        }

        if (isLocked) {
            if (surplus < 2) {
                throw new PanicError();
            }
        } else {
            if (surplus == 0) {
                throw new PanicError();
            }
        }

        surplus--;
    }

    @Override
    public boolean ___isReadBiased() {
        return isReadBiased;
    }

    @Override
    public boolean ___isLocked() {
        return isLocked;
    }

    @Override
    public boolean ___tryUpdateLock(int spinCount) {
        if (isLocked) {
            return false;
        }

        if (surplus == 0) {
            throw new PanicError();
        }

        isLocked = true;
        return true;
    }

    @Override
    public void ___unlockAfterBecomingReadBiased() {
        if (!isLocked) {
            throw new PanicError();
        }

        isLocked = false;
    }

    @Override
    public boolean ___arriveAndLockForUpdate(int spinCount) {
        throw new TodoException();
    }
}

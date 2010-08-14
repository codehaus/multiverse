package org.multiverse.stms.beta.conflictcounters;

import org.multiverse.api.exceptions.TodoException;
import org.multiverse.stms.beta.BetaTransactionalObject;

/**
 * @author Peter Veentjer
 */
public final class LocalConflictCounter {

    private final GlobalConflictCounter globalConflictCounter;
    private final long[] array;
    private long localConflictCount;

    public LocalConflictCounter(GlobalConflictCounter globalConflictCounter) {
        if (globalConflictCounter == null) {
            throw new NullPointerException();
        }
        this.globalConflictCounter = globalConflictCounter;
        this.array = new long[globalConflictCounter.getLength()];
        reset();
    }

    public GlobalConflictCounter getGlobalConflictCounter() {
        return globalConflictCounter;
    }

    public boolean syncAndCheckConflict() {
        if (array.length == 1) {
            long globalConflictCount = globalConflictCounter.getConflictCount(0);
            if (globalConflictCount != localConflictCount) {
                localConflictCount = globalConflictCount;
                return true;
            } else {
                return false;
            }
        }

        boolean conflict = false;

        for (int k = 0; k < array.length; k++) {
            long local = array[k];
            if (local >= 0) {
                long global = globalConflictCounter.getConflictCount(k);

                if (global != local) {
                    conflict = true;
                }

                array[k] = global;
            }
        }

        return conflict;
    }

    public long get() {
        if (array.length == 1) {
            return localConflictCount;
        }

        long result = 0;
        for (int k = 0; k < array.length; k++) {
            result += array[k];
        }
        return result;

    }

    public void arrive(BetaTransactionalObject ref) {
        throw new TodoException();
    }

    public void reset() {
        if (array.length == 1) {
            localConflictCount = globalConflictCounter.getConflictCount(0);
            return;
        }

        System.out.println("long conflict counter reset");
        System.arraycopy(globalConflictCounter.getInitArray(), 0, array, 0, array.length);
    }
}

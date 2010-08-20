package org.multiverse.stms.beta.conflictcounters;

import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Peter Veentjer
 */
public final class GlobalConflictCounter {

    private final AtomicLong[] counters;
    private final int[] initArray;

    public GlobalConflictCounter(int length) {
        this.counters = new AtomicLong[length];
        this.initArray = new int[length];

        for (int k = 0; k < length; k++) {
            counters[k] = new AtomicLong();
            initArray[k] = -1;
        }
    }

    public void signalConflict(BetaTransactionalObject ref) {
        int index = getIndex(ref);
        if (counters.length == 1) {
            index = 0;
        } else {
            index = getIndex(ref);
        }

        AtomicLong counter = counters[index];
        long oldValue = counter.get();
        counter.compareAndSet(oldValue, oldValue + 1);
    }

    private int getIndex(BetaTransactionalObject ref) {
        return 0;  //To change body of created methods use File | Settings | File Templates.
    }

    public LocalConflictCounter createLocalConflictCounter() {
        return new LocalConflictCounter(this);
    }

    protected int[] getInitArray() {
        return initArray;
    }

    public int getLength() {
        return counters.length;
    }

    public long count() {
        long result = 0;
        for (int k = 0; k < counters.length; k++) {
            result += counters[k].get();
        }
        return result;
    }

    public long getConflictCount(int counter) {
        return counters[counter].get();
    }
}

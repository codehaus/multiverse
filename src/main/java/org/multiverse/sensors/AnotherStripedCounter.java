package org.multiverse.sensors;

import java.util.concurrent.atomic.AtomicLong;

public class AnotherStripedCounter {

    private final AtomicLong[] array;

    public AnotherStripedCounter(int length) {
        array = new AtomicLong[length];
        for (int k = 0; k < length; k++) {
            array[k] = new AtomicLong();
        }
    }

    public long get() {
        long sum = 0;
        for (int k = 0; k < array.length; k++) {
            sum += array[k].get();
        }
        return sum;
    }

    public void incAtIndex(int index, long amount){
        array[index].addAndGet(amount);
    }
}

package org.multiverse.stms.alpha.transactions;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

public final class OptimalSize {

    private final AtomicInteger size = new AtomicInteger();

    public OptimalSize(int initialSize) {
        this.size.set(initialSize);
    }

    public void set(int newValue){
        size.set(newValue);
    }

    public int get() {
        return size.get();
    }

    public void compareAndSet(int expected, int newSize){
        size.compareAndSet(expected, newSize);
    }

    public String toString(){
        return format("OptimalSize(size=%s)",size.get());
    }
}

package org.multiverse.stms.gamma;

import org.multiverse.stms.gamma.transactionalobjects.GammaObject;

import java.util.concurrent.atomic.AtomicLong;

public class GlobalConflictCounter {

    private final AtomicLong counter = new AtomicLong();

    public void signalConflict(GammaObject object) {
        long count = counter.get();
        counter.compareAndSet(count, count + 1);
    }

    public long count() {
        return counter.get();
    }
}

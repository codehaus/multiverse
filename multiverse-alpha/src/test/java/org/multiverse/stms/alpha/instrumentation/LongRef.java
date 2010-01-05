package org.multiverse.stms.alpha.instrumentation;

import org.multiverse.transactional.annotations.TransactionalObject;

@TransactionalObject
public class LongRef {
    private long value;

    public LongRef() {
        value = 0;
    }

    public LongRef(long value) {
        this.value = value;
    }

    public long get() {
        return value;
    }

    public void inc() {
        value++;
    }

    public void set(long value) {
        this.value = value;
    }
}

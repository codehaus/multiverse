package org.multiverse.datastructures.refs;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.api.annotations.AtomicObject;

@AtomicObject
public class IntRef {

    private int value;

    public IntRef() {
        this(0);
    }

    public IntRef(int value) {
        this.value = value;
    }

    public int dec() {
        value--;
        return value;
    }

    public int inc() {
        int v = value;
        v++;
        value = v;
        return v;
    }

    public int set(int newValue) {
        int oldValue = value;
        value = newValue;
        return oldValue;
    }

    //@AtomicMethod(readonly = true)
    public int get() {
        return value;
    }

    //@AtomicMethod(readonly = true)
    public void await(int desiredValue) {
        if (value != desiredValue) {
            retry();
        }
    }

    public void setValue(int value) {
        this.value = value;
    }
}

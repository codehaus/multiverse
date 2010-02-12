package org.multiverse.transactional;

import org.multiverse.annotations.Commute;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

@TransactionalObject
public class Size {

    private int value;

    public Size(int value) {
        this.value = value;
    }

    @Commute
    public void inc() {
        value++;
    }

    @Commute
    public void dec() {
        value--;
    }

    @TransactionalMethod(readonly = true)
    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }
}

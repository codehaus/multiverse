package org.multiverse.integrationtests;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static org.multiverse.api.StmUtils.retry;

/**
 * @author Peter Veentjer
 */
@TransactionalObject
public class Ref {

    private int value;

    public Ref() {
        this(0);
    }

    public Ref(int value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = false)
    public void inc() {
        value++;
    }

    @TransactionalMethod(readonly = true)
    public int get() {
        return value;
    }

    @TransactionalMethod(readonly = false)
    public void set(int value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = false)
    public boolean compareAndSet(int expected, int update) {
        if (value != expected) {
            return false;
        }

        value = update;
        return true;
    }

    @TransactionalMethod(readonly = true)
    public void await(int expected) {
        if (expected != value) {
            retry();
        }
    }
}

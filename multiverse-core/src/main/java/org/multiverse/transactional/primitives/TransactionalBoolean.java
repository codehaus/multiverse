package org.multiverse.transactional.primitives;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static org.multiverse.api.StmUtils.retry;

/**
 * @author Peter Veentjer
 */
@TransactionalObject
public final class TransactionalBoolean {

    private boolean value;

    /**
     * Creates a new TransactionalBoolean with false as va
     */
    public TransactionalBoolean() {
        this(false);
    }

    public TransactionalBoolean(boolean value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public boolean get() {
        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public void await(boolean desired) {
        if (desired != value) {
            retry();
        }
    }

    /**
     * Sets the new value and returns the old value.
     *
     * @param newValue the new value.
     * @return the previous value.
     */
    public boolean set(boolean newValue) {
        boolean oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        if (value) {
            return "TransactionalBoolean(value=true)";
        } else {
            return "TransactionalBoolean(value=false)";
        }
    }

    @TransactionalMethod(readonly = true)
    public int hashCode() {
        return value ? 1 : 0;
    }

    @TransactionalMethod(readonly = true)
    public boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof TransactionalBoolean)) {
            return false;
        }

        TransactionalBoolean that = (TransactionalBoolean) thatObj;
        return that.value == this.value;
    }
}

package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static org.multiverse.api.StmUtils.retry;

/**
 * A Ref for storing a boolean.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public class BooleanRef {

    private boolean value;

    /**
     * Creates a new BooleanRef with false as va
     */
    public BooleanRef() {
        this(false);
    }

    public BooleanRef(boolean value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public final boolean get() {
        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final void await(boolean desired) {
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
    @TransactionalMethod(readonly = false, trackReads = true)
    public final boolean set(boolean newValue) {
        if (newValue == value) {
            return newValue;
        }

        boolean oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        if (value) {
            return "BooleanRef(value=true)";
        } else {
            return "BooleanRef(value=false)";
        }
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final int hashCode() {
        return value ? 1 : 0;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof BooleanRef)) {
            return false;
        }

        BooleanRef that = (BooleanRef) thatObj;
        return that.value == this.value;
    }
}

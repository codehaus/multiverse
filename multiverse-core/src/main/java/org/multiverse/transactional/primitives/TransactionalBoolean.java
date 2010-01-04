package org.multiverse.transactional.primitives;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.annotations.TransactionalObject;

/**
 * @author Peter Veentjer
 */
@TransactionalObject
public class TransactionalBoolean {

    private boolean value;

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

    @TransactionalMethod(readonly = true, automaticReadTracking = true)
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
        if(value){
            return "TransactionalBoolean(value=true)";
        }else{
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

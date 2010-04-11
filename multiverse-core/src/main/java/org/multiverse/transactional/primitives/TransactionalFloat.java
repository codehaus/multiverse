package org.multiverse.transactional.primitives;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.Float.floatToIntBits;
import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A reference for a primitive float.
 *
 * @author Peter Veentjer.
 */
@TransactionalObject
public final class TransactionalFloat {

    private float value;

    public TransactionalFloat() {
        this(0f);
    }

    public TransactionalFloat(float value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public float get() {
        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public void await(char desired) {
        if (floatToIntBits(this.value) != floatToIntBits(desired)) {
            retry();
        }
    }

    /**
     * Sets the new value and returns the old value.
     *
     * @param newValue the new value.
     * @return the previous value.
     */
    public float set(float newValue) {
        float oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    public float inc() {
        value++;
        return value;
    }

    public float inc(float amount) {
        value += amount;
        return value;
    }

    public float dec() {
        value--;
        return value;
    }

    public float dec(float amount) {
        value -= amount;
        return value;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        return format("TransactionalFloat(value=%s)", value);
    }

    @TransactionalMethod(readonly = true)
    public int hashCode() {
        return floatToIntBits(value);
    }

    @TransactionalMethod(readonly = true)
    public boolean equals(Object thatobj) {
        if (thatobj == this) {
            return true;
        }

        if (!(thatobj instanceof TransactionalFloat)) {
            return false;
        }

        TransactionalFloat that = (TransactionalFloat) thatobj;
        return floatToIntBits(this.value) == floatToIntBits(that.value);
    }
}

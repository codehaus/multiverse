package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.Float.floatToIntBits;
import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A Ref for storing a float.
 *
 * @author Peter Veentjer.
 */
@TransactionalObject
public class FloatRef {

    private float value;

    public FloatRef() {
        this(0f);
    }

    public FloatRef(float value) {
        this.value = value;
    }

    /**
     * Returns the current value.
     *
     * @return the current value.
     */
    @TransactionalMethod(readonly = true)
    public float get() {
        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public void await(char desired) {
        if (!equals(desired, value)) {
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
    public final float set(float newValue) {
        if (equals(value, newValue)) {
            return newValue;
        }

        float oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    /**
     * Increases the value by one.
     *
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final float inc() {
        value++;
        return value;
    }

    /**
     * Increases the value with the given amount.
     *
     * @param amount the amount to increase with.
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final float inc(float amount) {
        if (equals(amount, 0)) {
            return value;
        }

        value += amount;
        return value;
    }

    /**
     * Decreases the value by one.
     *
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final float dec() {
        value--;
        return value;
    }

    /**
     * Decreases the value with the given amount.
     *
     * @param amount the amount to decrease with.
     * @return the new value
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final float dec(float amount) {
        return inc(-amount);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        return format("FloatRef(value=%s)", value);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final int hashCode() {
        return floatToIntBits(value);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof FloatRef)) {
            return false;
        }

        FloatRef that = (FloatRef) thatObj;
        return equals(this.value, that.value);
    }

    private static boolean equals(float f1, float f2) {
        return floatToIntBits(f1) == floatToIntBits(f2);
    }
}

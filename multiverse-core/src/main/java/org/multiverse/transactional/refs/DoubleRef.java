package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.Double.doubleToLongBits;
import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A Ref for storing a double.
 *
 * @author Peter Veentjer.
 */
@TransactionalObject
public class DoubleRef {

    private double value;

    public DoubleRef() {
        this((double) 0);
    }

    public DoubleRef(double value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public final double get() {
        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final void await(double desired) {
        if (!equals(value, desired)) {
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
    public final double set(double newValue) {
        if (equals(newValue, value)) {
            return newValue;
        }

        double oldValue = value;
        value = newValue;
        return oldValue;
    }

    /**
     * Increases the current value by one.
     *
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final double inc() {
        value++;
        return value;
    }

    /**
     * Increases the value with the given amount.
     *
     * @param amount the amount to increase with.
     * @return the new value
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final double inc(double amount) {
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
    public final double dec() {
        value--;
        return value;
    }

    /**
     * Decreases the value with the given amount.
     *
     * @param amount the amount to decrease with.
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final double dec(double amount) {
        if (equals(amount, 0)) {
            return value;
        }

        value -= amount;
        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        return format("DoubleRef(value=%s)", value);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final int hashCode() {
        long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final boolean equals(Object thatobj) {
        if (thatobj == this) {
            return true;
        }

        if (!(thatobj instanceof DoubleRef)) {
            return false;
        }

        DoubleRef that = (DoubleRef) thatobj;
        return doubleToLongBits(this.value) == doubleToLongBits(that.value);
    }

    private static boolean equals(double d1, double d2) {
        return doubleToLongBits(d1) == doubleToLongBits(d2);
    }
}

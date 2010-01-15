package org.multiverse.transactional.primitives;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.Double.doubleToLongBits;
import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A reference for a primitive double.
 *
 * @author Peter Veentjer.
 */
@TransactionalObject
public class TransactionalDouble {

    private double value;

    public TransactionalDouble() {
        this((double) 0);
    }

    public TransactionalDouble(double value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public double get() {
        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public void await(double desired) {
        if (doubleToLongBits(this.value) != doubleToLongBits(desired)) {
            retry();
        }
    }

    /**
     * Sets the new value and returns the old value.
     *
     * @param newValue the new value.
     * @return the previous value.
     */
    public double set(double newValue) {
        double oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    public double inc() {
        value++;
        return value;
    }

    public double inc(double amount) {
        value += amount;
        return value;
    }

    public double dec() {
        value--;
        return value;
    }

    public double dec(double amount) {
        value -= amount;
        return value;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        return format("TransactionalDouble(value=%s)", value);
    }

    @TransactionalMethod(readonly = true)
    public int hashCode() {
        long bits = Double.doubleToLongBits(value);
        return (int) (bits ^ (bits >>> 32));
    }

    @TransactionalMethod(readonly = true)
    public boolean equals(Object thatobj) {
        if (thatobj == this) {
            return true;
        }

        if (!(thatobj instanceof TransactionalDouble)) {
            return false;
        }

        TransactionalDouble that = (TransactionalDouble) thatobj;
        return doubleToLongBits(this.value) == doubleToLongBits(that.value);
    }
}

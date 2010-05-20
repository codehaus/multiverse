package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A Ref for storing a short.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public class ShortRef {

    private short value;

    public ShortRef() {
        this((short) 0);
    }

    public ShortRef(short value) {
        this.value = value;
    }

    /**
     * Gets the current value.
     *
     * @return the current value.
     */
    @TransactionalMethod(readonly = true)
    public final short get() {
        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final void await(short desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final short awaitLargerThan(short than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final short awaitLargerOrEqualThan(short than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final short awaitSmallerThan(short than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final short awaitSmallerOrEqualThan(short than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final short awaitNotEqualTo(short than) {
        if (!(value != than)) {
            retry();
        }

        return value;
    }

    /**
     * Sets the new value and returns the old value.
     *
     * @param newValue the new value.
     * @return the previous value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public short set(short newValue) {
        if (newValue == value) {
            return newValue;
        }

        short oldValue = value;
        value = newValue;
        return oldValue;
    }

    /**
     * Increases the value by one.
     *
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final short inc() {
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
    public final short inc(short amount) {
        if (amount == 0) {
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
    public final short dec() {
        value--;
        return value;
    }

    /**
     * Increases the value with the given amount.
     *
     * @param amount the amount to increase with
     * @return the new value
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final short dec(short amount) {
        if (amount == 0) {
            return value;
        }

        value -= amount;
        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        return format("ShortRef(value=%s)", value);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final int hashCode() {
        return (int) value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof ShortRef)) {
            return false;
        }

        ShortRef that = (ShortRef) thatObj;
        return that.value == this.value;
    }
}

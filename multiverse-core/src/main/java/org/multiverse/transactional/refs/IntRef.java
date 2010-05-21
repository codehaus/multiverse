package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A Ref for storing an int.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public class IntRef {

    private int value;

    /**
     * Creates a new IntRef with the 0 as value.
     */
    public IntRef() {
        this(0);
    }

    /**
     * Creates a new IntRef with the given value.
     *
     * @param value the initial value of this IntRef.
     */
    public IntRef(int value) {
        this.value = value;
    }

    /**
     * Gets the current value.
     *
     * @return gets the current value.
     */
    @TransactionalMethod(readonly = true)
    public final int get() {
        return value;
    }

    /**
     * Sets the new value and returns the old value.
     *
     * @param newValue the new value.
     * @return the previous value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final int set(int newValue) {
        int oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    /**
     * Decreases the value in this IntRef by one.
     *
     * @return the decreased value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final int dec() {
        value--;
        return value;
    }

    /**
     * Increases the value in this IntRef by one.
     *
     * @return the increased value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final int inc() {
        value++;
        return value;
    }

    /**
     * Increase the value of this IntRef by the given amount.
     *
     * @param amount the amount the value needs to be increased with. Value is allowed to be 0 or negative.
     * @return the increased value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final int inc(int amount) {
        if (amount == 0) {
            return value;
        }
        value += amount;
        return value;
    }

    /**
     * Decreases the value of this IntRef by the given amount.
     *
     * @param amount the amount the value needs to be decreased with. Value is allowed to be 0 or negative.
     * @return the decreased value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final int dec(int amount) {
        return inc(-amount);
    }

    /**
     * Waits till the value is equal to the desired value.
     *
     * @param desired the value to wait for.
     */
    @TransactionalMethod(readonly = true, trackReads = true)
    public final void await(int desired) {
        if (desired != value) {
            retry();
        }
    }

    /**
     * Waits till this value is larger than.
     *
     * @param than the value to wait for.
     * @return the value that currently is active.
     */
    @TransactionalMethod(readonly = true, trackReads = true)
    public final int awaitLargerThan(int than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    /**
     * Waits till the value is equal or larger than.
     *
     * @param than
     * @return
     */
    @TransactionalMethod(readonly = true, trackReads = true)
    public final int awaitLargerOrEqualThan(int than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final int awaitSmallerThan(int than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final int awaitSmallerOrEqualThan(int than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final int awaitNotEqualTo(int than) {
        if (!(value != than)) {
            retry();
        }

        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        return format("IntRef(value=%s)", value);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final int hashCode() {
        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof IntRef)) {
            return false;
        }

        IntRef that = (IntRef) thatObj;
        return that.value == this.value;
    }
}

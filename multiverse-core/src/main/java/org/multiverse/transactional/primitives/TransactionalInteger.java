package org.multiverse.transactional.primitives;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A transactional primitive for a int.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class TransactionalInteger {

    private int value;

    /**
     * Creates a new TransactionalInteger with the 0 as value.
     */
    public TransactionalInteger() {
        this(0);
    }

    /**
     * Creates a new TransactionalInteger with the given value.
     *
     * @param value the initial value of this TransactionalInteger.
     */
    public TransactionalInteger(int value) {
        this.value = value;
    }

    /**
     * Gets the current value.
     *
     * @return gets the current value.
     */
    @TransactionalMethod(readonly = true)
    public int get() {
        return value;
    }

    /**
     * Sets the new value and returns the old value.
     *
     * @param newValue the new value.
     * @return the previous value.
     */
    public int set(int newValue) {
        int oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    /**
     * Decreases the value in this TransactionalInteger by one.
     *
     * @return the decreased value.
     */
    public int dec() {
        value--;
        return value;
    }

    /**
     * Increases the value in this TransactionalInteger by one.
     *
     * @return the increased value.
     */
    public int inc() {
        value++;
        return value;
    }

    /**
     * Increase the value of this TransactionalInteger by the given amount.
     *
     * @param amount the amount the value needs to be increased with. Value is allowed to be 0 or negative.
     * @return the increased value.
     */
    public int inc(int amount) {
        value += amount;
        return value;
    }

    /**
     * Decreases the value of this TransactionalInteger by the given amount.
     *
     * @param amount the amount the value needs to be decreased with. Value is allowed to be 0 or negative.
     * @return the decreased value.
     */
    public int dec(int amount) {
        value -= amount;
        return value;
    }

    /**
     * Waits till the value is equal to the desired value.
     *
     * @param desired the value to wait for.
     */
    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public void await(int desired) {
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
    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public int awaitLargerThan(int than) {
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
    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public int awaitLargerOrEqualThan(int than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public int awaitSmallerThan(int than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public int awaitSmallerOrEqualThan(int than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public int awaitNotEqualTo(int than) {
        if (!(value != than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        return format("TransactionalInteger(value=%s)", value);
    }

    @TransactionalMethod(readonly = true)
    public int hashCode() {
        return value;
    }

    @TransactionalMethod(readonly = true)
    public boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof TransactionalInteger)) {
            return false;
        }

        TransactionalInteger that = (TransactionalInteger) thatObj;
        return that.value == this.value;
    }
}

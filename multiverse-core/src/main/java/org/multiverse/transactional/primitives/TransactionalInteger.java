package org.multiverse.transactional.primitives;

import static org.multiverse.api.StmUtils.retry;

import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.annotations.TransactionalObject;

import static java.lang.String.format;

/**
 * @author Peter Veentjer
 */
@TransactionalObject
public class TransactionalInteger {

    private int value;

    public TransactionalInteger() {
        this(0);
    }

    public TransactionalInteger(int value) {
        this.value = value;
    }

    public int inc() {
        value++;
        return value;
    }

    public int inc(int amount) {
        value += amount;
        return value;
    }

    public int dec() {
        value--;
        return value;
    }

    public int dec(int amount) {
        value -= amount;
        return value;
    }

    @TransactionalMethod(readonly = true)
    public int get() {
        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public void await(int desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public int awaitLargerThan(int than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

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

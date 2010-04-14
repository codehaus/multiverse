package org.multiverse.transactional.primitives;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A reference for a primitive short.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class TransactionalShort {

    //todo: make private again
    public short value;

    public TransactionalShort() {
        this((short) 0);
    }

    public TransactionalShort(short value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public short get() {
        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public void await(short desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public short awaitLargerThan(short than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public short awaitLargerOrEqualThan(short than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public short awaitSmallerThan(short than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public short awaitSmallerOrEqualThan(short than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public short awaitNotEqualTo(short than) {
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
    public short set(short newValue) {
        short oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    public short inc() {
        value++;
        return value;
    }

    public short inc(short amount) {
        value += amount;
        return value;
    }

    public short dec() {
        value--;
        return value;
    }

    public short dec(short amount) {
        this.value -= amount;
        return value;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        return format("TransactionalShort(value=%s)", value);
    }

    @TransactionalMethod(readonly = true)
    public int hashCode() {
        return (int) value;
    }

    @TransactionalMethod(readonly = true)
    public boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof TransactionalShort)) {
            return false;
        }

        TransactionalShort that = (TransactionalShort) thatObj;
        return that.value == this.value;
    }
}

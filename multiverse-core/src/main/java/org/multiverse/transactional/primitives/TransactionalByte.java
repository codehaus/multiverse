package org.multiverse.transactional.primitives;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A reference for a primitive byte.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class TransactionalByte {

    //todo: make private again
    public byte value;

    public TransactionalByte() {
        this((byte) 0);
    }

    public TransactionalByte(byte value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public byte get() {
        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public void await(byte desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public byte awaitLargerThan(byte than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public byte awaitLargerOrEqualThan(byte than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public byte awaitSmallerThan(byte than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public byte awaitSmallerOrEqualThan(byte than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public byte awaitNotEqualTo(byte than) {
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
    public byte set(byte newValue) {
        byte oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    public byte inc() {
        value++;
        return value;
    }

    public byte inc(byte amount) {
        value += amount;
        return value;
    }

    public byte dec() {
        value--;
        return value;
    }

    public byte dec(byte amount) {
        value -= amount;
        return value;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        return format("TransactionalByte(value=%s)", value);
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

        if (!(thatObj instanceof TransactionalByte)) {
            return false;
        }

        TransactionalByte that = (TransactionalByte) thatObj;
        return that.value == this.value;
    }
}

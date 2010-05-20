package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A Ref for storing a byte.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public class ByteRef {

    private byte value;

    public ByteRef() {
        this((byte) 0);
    }

    public ByteRef(byte value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public final byte get() {
        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final void await(byte desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final byte awaitLargerThan(byte than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final byte awaitLargerOrEqualThan(byte than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final byte awaitSmallerThan(byte than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final byte awaitSmallerOrEqualThan(byte than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final byte awaitNotEqualTo(byte than) {
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
    public final byte set(byte newValue) {
        if (newValue == value) {
            return newValue;
        }

        byte oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    /**
     * Increases the value by one.
     *
     * @return the new value
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final byte inc() {
        value++;
        return value;
    }

    /**
     * Increases the value with the given amount.
     *
     * @param amount the amount to increae the value with.
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final byte inc(byte amount) {
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
    public final byte dec() {
        value--;
        return value;
    }

    /**
     * Decreases the value with the given amount.
     *
     * @param amount the amount to decrease the value with.
     * @return the new value
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final byte dec(byte amount) {
        if (amount == 0) {
            return value;
        }

        value -= amount;
        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        return format("ByteRef(value=%s)", value);
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

        if (!(thatObj instanceof ByteRef)) {
            return false;
        }

        ByteRef that = (ByteRef) thatObj;
        return that.value == this.value;
    }
}

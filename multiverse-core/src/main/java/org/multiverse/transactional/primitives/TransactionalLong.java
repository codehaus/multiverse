package org.multiverse.transactional.primitives;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A reference for a long.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class TransactionalLong {

    //todo: make private again
    public long value;

    public TransactionalLong() {
        this(0L);
    }

    public TransactionalLong(long value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public long get() {
        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public void await(long desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public long awaitLargerThan(long than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public long awaitLargerOrEqualThan(long than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public long awaitSmallerThan(long than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public long awaitSmallerOrEqualThan(long than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public long awaitNotEqualThan(long than) {
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
    public long set(long newValue) {
        long oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    public long inc() {
        value++;
        return value;
    }

    public long inc(long amount) {
        value += amount;
        return value;
    }

    public long dec() {
        value--;
        return value;
    }

    public long dec(long amount) {
        value -= amount;
        return value;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        return format("TransactionalLong(value=%s)", value);
    }

    @TransactionalMethod(readonly = true)
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    @TransactionalMethod(readonly = true)
    public boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof TransactionalLong)) {
            return false;
        }

        TransactionalLong that = (TransactionalLong) thatObj;
        return that.value == this.value;
    }
}

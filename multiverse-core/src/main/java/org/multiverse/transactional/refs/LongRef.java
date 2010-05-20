package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A Ref for storing a long.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public class LongRef {

    private long value;

    public LongRef() {
        this(0L);
    }

    public LongRef(long value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public final long get() {
        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final void await(long desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final long awaitLargerThan(long than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final long awaitLargerOrEqualThan(long than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final long awaitSmallerThan(long than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final long awaitSmallerOrEqualThan(long than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final long awaitNotEqualThan(long than) {
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
    public final long set(long newValue) {
        long oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    @TransactionalMethod(readonly = false, trackReads = true)
    public final long inc() {
        value++;
        return value;
    }

    @TransactionalMethod(readonly = false, trackReads = true)
    public final long inc(long amount) {
        value += amount;
        return value;
    }

    @TransactionalMethod(readonly = false, trackReads = true)
    public final long dec() {
        value--;
        return value;
    }

    @TransactionalMethod(readonly = false, trackReads = true)
    public final long dec(long amount) {
        value -= amount;
        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        return format("LongRef(value=%s)", value);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final int hashCode() {
        return (int) (value ^ (value >>> 32));
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final boolean equals(Object thatObj) {
        if (thatObj == this) {
            return true;
        }

        if (!(thatObj instanceof LongRef)) {
            return false;
        }

        LongRef that = (LongRef) thatObj;
        return that.value == this.value;
    }
}

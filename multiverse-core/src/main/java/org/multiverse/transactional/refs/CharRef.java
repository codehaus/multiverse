package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A Ref for storing a char.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public class CharRef {

    private char value;

    public CharRef() {
        this((char) 0);
    }

    public CharRef(char value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public final char get() {
        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final void await(char desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final char awaitLargerThan(char than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final char awaitLargerOrEqualThan(char than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final char awaitSmallerThan(char than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final char awaitSmallerOrEqualThan(char than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public final char awaitNotEqualTo(char than) {
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
    public final char set(char newValue) {
        if (newValue == value) {
            return newValue;
        }

        char oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    /**
     * Increases the value by one.
     *
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final char inc() {
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
    public final char inc(char amount) {
        if (amount == 0) {
            return value;
        }

        value += amount;
        return value;
    }

    /**
     * Decreases the value by one.
     *
     * @return the new value
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final char dec() {
        value--;
        return value;
    }

    /**
     * Decreases the value with the given amount.
     *
     * @param amount the amount to decrease the value with.
     * @return the new value.
     */
    @TransactionalMethod(readonly = false, trackReads = true)
    public final char dec(char amount) {
        if (amount == 0) {
            return value;
        }

        value -= amount;
        return this.value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        return format("CharRef(value=%s)", value);
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

        if (!(thatObj instanceof CharRef)) {
            return false;
        }

        CharRef that = (CharRef) thatObj;
        return that.value == this.value;
    }
}

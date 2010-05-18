package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A reference for a primitive char.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class CharRef {

    private char value;

    public CharRef() {
        this((char) 0);
    }

    public CharRef(char value) {
        this.value = value;
    }

    @TransactionalMethod(readonly = true)
    public char get() {
        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public void await(char desired) {
        if (desired != value) {
            retry();
        }
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public char awaitLargerThan(char than) {
        if (!(value > than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public char awaitLargerOrEqualThan(char than) {
        if (!(value >= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public char awaitSmallerThan(char than) {
        if (!(value < than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public char awaitSmallerOrEqualThan(char than) {
        if (!(value <= than)) {
            retry();
        }

        return value;
    }

    @TransactionalMethod(readonly = true, trackReads = true)
    public char awaitNotEqualTo(char than) {
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
    public char set(char newValue) {
        char oldValue = this.value;
        this.value = newValue;
        return oldValue;
    }

    public char inc() {
        value++;
        return value;
    }

    public char inc(char amount) {
        value += amount;
        return value;
    }

    public char dec() {
        value--;
        return value;
    }

    public char dec(char amount) {
        value -= amount;
        return this.value;
    }

    @TransactionalMethod(readonly = true)
    public String toString() {
        return format("CharRef(value=%s)", value);
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

        if (!(thatObj instanceof CharRef)) {
            return false;
        }

        CharRef that = (CharRef) thatObj;
        return that.value == this.value;
    }
}

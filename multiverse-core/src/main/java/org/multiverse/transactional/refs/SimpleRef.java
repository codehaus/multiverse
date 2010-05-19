package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * Default {@link org.multiverse.transactional.refs.Ref}. Changes on refs are atomic and consistent. In most cases this is the
 * Ref implementation you want to use.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class SimpleRef<E> implements Ref<E> {

    private E value;

    /**
     * Creates a SimpleRef with a null reference.
     */
    public SimpleRef() {
        this(null);
    }


    /**
     * Creates a new SimpleRef with the provided reference. This reference is allowed to
     * be null.
     *
     * @param value the reference to store in this SimpleRef.
     */
    public SimpleRef(E value) {
        this.value = value;
    }

    @Override
    @TransactionalMethod(readonly = true, trackReads = true)
    public E getOrAwait() {
        if (value == null) {
            retry();
        }

        return value;
    }

    @Override
    @TransactionalMethod(readonly = true, trackReads = true, interruptible = true)
    public E getOrAwaitInterruptibly() throws InterruptedException {
        if (value == null) {
            retry();
        }

        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public boolean isNull() {
        return value == null;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public E get() {
        return value;
    }

    @Override
    @TransactionalMethod(trackReads = true)
    public E set(E newValue) {
        E initialValue = value;

        //optimization to prevent loading the object if not needed.
        if (initialValue == newValue) {
            return newValue;
        }

        value = newValue;
        return initialValue;
    }

    @Override
    public E clear() {
        return set(null);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public String toString() {
        if (value == null) {
            return "SimpleRef(ref=null)";
        } else {
            return format("SimpleRef(ref=%s)", value);
        }
    }
}

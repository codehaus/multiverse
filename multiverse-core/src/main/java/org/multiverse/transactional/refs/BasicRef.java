package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * Basic {@link org.multiverse.transactional.refs.Ref} implementation. In most cases this is the
 * Ref implementation you want to use.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public class BasicRef<E> implements Ref<E> {

    private E value;

    /**
     * Creates a BasicRef with a null reference.
     */
    public BasicRef() {
        this(null);
    }


    /**
     * Creates a new BasicRef with the provided reference. This reference is allowed to
     * be null.
     *
     * @param value the reference to store in this BasicRef.
     */
    public BasicRef(E value) {
        this.value = value;
    }

    @Override
    @TransactionalMethod(readonly = true, trackReads = true)
    public final E getOrAwait() {
        if (value == null) {
            retry();
        }

        return value;
    }

    @Override
    @TransactionalMethod(readonly = true, trackReads = true, interruptible = true)
    public final E getOrAwaitInterruptibly() throws InterruptedException {
        if (value == null) {
            retry();
        }

        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final boolean isNull() {
        return value == null;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final E get() {
        return value;
    }

    @Override
    @TransactionalMethod(trackReads = true)
    public final E set(E newValue) {
        if (value == newValue) {
            return newValue;
        }

        E oldValue = value;
        value = newValue;
        return oldValue;
    }

    @Override
    @TransactionalMethod(readonly = false, trackReads = true)
    public final E clear() {
        return set(null);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public final String toString() {
        if (value == null) {
            return "BasicRef(ref=null)";
        } else {
            return format("BasicRef(ref=%s)", value);
        }
    }
}

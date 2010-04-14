package org.multiverse.transactional;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * Default {@link TransactionalReference}. Changes on primitives are atomic and consistent,
 * but not completely because a transaction could suffer from the ABA problem between transactions.
 * See the {@link TransactionalAbaReference} to solve this problem.
 * <p/>
 * It depends on the STM implementation if the ABA problem can occur btw. If the readset also
 * is included in the conflict detection, then the ABA problem can't occur.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class DefaultTransactionalReference<E> implements TransactionalReference<E> {
    //todo: should be made private again
    public E value;

    /**
     * Creates a DefaultTransactionalReference with a null reference.
     */
    public DefaultTransactionalReference() {
        this(null);
    }


    /**
     * Creates a new DefaultTransactionalReference with the provided reference. This reference is allowed to
     * be null.
     *
     * @param value the reference to store in this DefaultTransactionalReference.
     */
    public DefaultTransactionalReference(E value) {
        this.value = value;
    }

    @Override
    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true)
    public E getOrAwait() {
        if (value == null) {
            retry();
        }

        return value;
    }

    @Override
    @TransactionalMethod(readonly = true, automaticReadTrackingEnabled = true, interruptible = true)
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
    @TransactionalMethod(automaticReadTrackingEnabled = true)
    public E set(E newValue) {
        E currentValue = value;

        //optimization to prevent loading the object if not needed.
        if (currentValue == newValue) {
            return newValue;
        }

        E oldRef = currentValue;
        value = newValue;
        return oldRef;
    }

    @Override
    public E clear() {
        return set(null);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public String toString() {
        if (value == null) {
            return "DefaultTransactionalReference(ref=null)";
        } else {
            return format("DefaultTransactionalReference(ref=%s)", value);
        }
    }
}

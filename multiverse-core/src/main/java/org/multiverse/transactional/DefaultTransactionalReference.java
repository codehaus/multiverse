package org.multiverse.transactional;

import org.multiverse.api.Transaction;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

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
    private E reference;

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
     * @param reference the reference to store in this DefaultTransactionalReference.
     */
    public DefaultTransactionalReference(E reference) {
        this.reference = reference;
    }

    @Override
    @TransactionalMethod(readonly = true, automaticReadTracking = true)
    public E getOrAwait() {
        if (reference == null) {
            retry();
        }

        return reference;
    }

    @Override
    @TransactionalMethod(readonly = true, automaticReadTracking = true, interruptible = true)
    public E getOrAwaitInterruptibly() throws InterruptedException {
        if (reference == null) {
            retry();
        }

        return reference;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public boolean isNull() {
        return reference == null;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public E get() {
        return reference;
    }

    @Override
    public E set(E newRef) {
        E oldRef = reference;
        this.reference = newRef;
        return oldRef;
    }

    @Override
    public E clear() {
        return set(null);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public String toString() {
        if (reference == null) {
            return "DefaultTransactionalReference(ref=null)";
        } else {
            return format("DefaultTransactionalReference(ref=%s)", reference);
        }
    }
}

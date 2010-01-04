package org.multiverse.transactional;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.transactional.TransactionalReference;

import static java.lang.String.format;

/**
 * A {@link TransactionalReference} that doesn't suffer from the ABA problem. See
 * the {@link DefaultTransactionalReference} for more information.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class TransactionalAbaReference<E> implements TransactionalReference<E> {

    private E reference;

    //although the writeVersion is only used for updating and never for reading,
    //the stm will do a read when it does a dirty check. So ignore pmd when it
    //complains that the field is 'unused'.
    private long writeVersion;

    /**
     * Creates a new TransactionalAbaReference with a null reference.
     */
    public TransactionalAbaReference() {
        this.reference = null;
        this.writeVersion = Long.MIN_VALUE;
    }

    /**
     * Creates a new TransactionalAbaReference with the provided reference. This reference is allowed
     * to be null.
     *
     * @param reference the reference to store.
     */
    public TransactionalAbaReference(E reference) {
        this.reference = reference;
        this.writeVersion = Long.MIN_VALUE;
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
    public E get() {
        return reference;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public boolean isNull() {
        return reference == null;
    }

    @Override
    public E set(E newRef) {
        if (newRef != reference) {
            E oldRef = reference;
            reference = newRef;
            writeVersion++;
            return oldRef;
        } else {
            return newRef;
        }
    }

    @Override
    public E clear() {
        return set(null);
    }

    @Override
    @TransactionalMethod(readonly = true)
    public String toString() {
        if (reference == null) {
            return "TransactionalAbaReference(ref=null)";
        } else {
            return format("TransactionalAbaReference(ref=%s)", reference);
        }
    }
}

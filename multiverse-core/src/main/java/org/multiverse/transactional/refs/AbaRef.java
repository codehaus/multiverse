package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * A {@link Ref} that doesn't suffer from the ABA problem. See
 * the {@link SimpleRef} for more information.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class AbaRef<E> implements Ref<E> {

    private E value;

    //although the writeVersion is only used for updating and never for reading,
    //the stm will do a read when it does a dirty check. So ignore pmd when it
    //complains that the field is 'unused'.
    private long writeVersion;

    /**
     * Creates a new TransactionalAbaReference with a null reference.
     */
    public AbaRef() {
        this.value = null;
        this.writeVersion = Long.MIN_VALUE;
    }

    /**
     * Creates a new TransactionalAbaReference with the provided reference. This reference is allowed
     * to be null.
     *
     * @param value the reference to store.
     */
    public AbaRef(E value) {
        this.value = value;
        this.writeVersion = Long.MIN_VALUE;
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
    public E get() {
        return value;
    }

    @Override
    @TransactionalMethod(readonly = true)
    public boolean isNull() {
        return value == null;
    }

    @Override
    public E set(E newRef) {
        if (newRef != value) {
            E initialValue = value;
            value = newRef;
            writeVersion++;
            return initialValue;
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
        if (value == null) {
            return "TransactionalAbaReference(ref=null)";
        } else {
            return format("TransactionalAbaReference(ref=%s)", value);
        }
    }
}

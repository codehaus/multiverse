package org.multiverse.datastructures.refs;

import static org.multiverse.api.StmUtils.retry;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.annotations.AtomicObject;

import static java.lang.String.format;

/**
 * A {@link org.multiverse.datastructures.refs.ManagedRef} that doesn't suffer from the ABA problem. See
 * the {@link Ref} for more information.
 *
 * @author Peter Veentjer
 */
@AtomicObject
public final class AbaRef<E> implements ManagedRef<E> {

    private E reference;

    //although the writeVersion is only used for updating and never for reading,
    //the stm will do a read when it does a dirty check. So ignore pmd when it
    //complains that the field is 'unused'.
    private long writeVersion;

    /**
     * Creates a new AbaRef with a null reference.
     */
    public AbaRef() {
        this.reference = null;
        this.writeVersion = Long.MIN_VALUE;
    }

    /**
     * Creates a new AbaRef with the provided reference. This reference is allowed
     * to be null.
     *
     * @param reference the reference to store.
     */
    public AbaRef(E reference) {
        this.reference = reference;
        this.writeVersion = Long.MIN_VALUE;
    }

    @Override
    @AtomicMethod(readonly = true)
    public E getOrAwait() {
        if (reference == null) {
            retry();
        }

        return reference;
    }

    @Override
    @AtomicMethod(readonly = true)
    public E get() {
        return reference;
    }

    @Override
    @AtomicMethod(readonly = true)
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
    @AtomicMethod(readonly = true)
    public String toString() {
        if (reference == null) {
            return "AbaRef(ref=null)";
        } else {
            return format("AbaRef(ref=%s)", reference);
        }
    }
}

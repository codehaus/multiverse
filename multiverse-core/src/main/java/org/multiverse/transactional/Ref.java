package org.multiverse.transactional;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

import static java.lang.String.format;
import static org.multiverse.api.StmUtils.retry;

/**
 * Default {@link TransactionalReference}. Changes on primitives are atomic and consistent,
 * but not completely because a transaction could suffer from the ABA problem between transactions.
 * See the {@link AbaRef} to solve this problem.
 * <p/>
 * It depends on the STM implementation if the ABA problem can occur btw. If the readset also
 * is included in the conflict detection, then the ABA problem can't occur.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public final class Ref<E> implements TransactionalReference<E> {

    private E value;

    /**
     * Creates a Ref with a null reference.
     */
    public Ref() {
        this(null);
    }


    /**
     * Creates a new Ref with the provided reference. This reference is allowed to
     * be null.
     *
     * @param value the reference to store in this Ref.
     */
    public Ref(E value) {
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
            return "Ref(ref=null)";
        } else {
            return format("Ref(ref=%s)", value);
        }
    }
}

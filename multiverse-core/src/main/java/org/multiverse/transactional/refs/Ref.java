package org.multiverse.transactional.refs;

import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;

/**
 * The Ref is responsible for storing a value while providing transactional guarantees (failure
 * atomicity, isolation)
 * <p/>
 * <p/>
 * Timed versions of the getOrAwait methods will be added in the near future.
 *
 * @author Peter Veentjer
 */
@TransactionalObject
public interface Ref<E> {

    /**
     * Gets the current value stored in this reference.
     *
     * @return the current value.
     */
    @TransactionalMethod(readonly = true)
    E get();

    /**
     * Gets the current non null value, or waits until a non null value comes available. So you will
     * always get a non null ref.
     * <p/>
     * This call is not interruptible, unless the enclosing transaction is configured as interruptible.
     *
     * @return the current stored reference.
     */
    @TransactionalMethod(readonly = true)
    E getOrAwait();

    /**
     * Gets the current non null value, or waits until a non null value becomes available or until
     * it is is Interrupted.
     * <p/>
     * This call is interruptible, unless the enclosing transaction isn't configured as interruptible.
     *
     * @return the current stored reference.
     * @throws InterruptedException if the thread is interrupted while waiting on a reference to come available.
     */
    @TransactionalMethod(readonly = true)
    E getOrAwaitInterruptibly() throws InterruptedException;

    /**
     * Sets the new value value. The value is allowed to be null.
     *
     * @param newValue the new value
     * @return the previous value, which could be null.
     */
    E set(E newValue);

    /**
     * Clears the value and returns the old value  (could be null).
     *
     * @return the old value.
     */
    E clear();

    /**
     * Checks if the value is null.
     *
     * @return true if the value is null, false otherwise.
     */
    @TransactionalMethod(readonly = true)
    boolean isNull();
}

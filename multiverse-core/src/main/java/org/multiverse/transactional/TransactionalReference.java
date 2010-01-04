package org.multiverse.transactional;

/**
 * The TransactionalReference is responsible for storing an object reference transactional (so that it can be
 * used in a {@link org.multiverse.api.Transaction}.
 *
 * <p/>
 * Timed versions of the getOrAwait methods will be added in the near future.
 *
 * @author Peter Veentjer
 */
public interface TransactionalReference<E> {

    /**
     * Gets the current stored ref, or null if no ref is stored.
     *
     * @return the current stored ref, or null if no ref is stored.
     */
    E get();

    /**
     * Gets the current stored ref, or waits until a non null reference becomes available. So you will always get
     * a non null ref.
     *
     * This call is not interruptible, unless the enclosing transaction is configured as interruptible.
     *
     * @return the current stored reference.
     */
    E getOrAwait();

    /**
     * Gets the current stored reference, or waits until a non null references becomes available or until
     * it is is Interrupted.
     *
     * This call is interruptible, unless the enclosing transaction isn't configured as interruptible.
     *
     * @return the current stored reference.
     * @throws InterruptedException if the thread is interrupted while waiting on a reference to come available.
     */
    E getOrAwaitInterruptibly() throws InterruptedException;

    /**
     * Sets the current ref. The ref is allowed to be null.
     *
     * @param ref the ref to set.
     * @return the previous ref, which could be null.
     */
    E set(E ref);

    /**
     * Clears the ref and returns the old value of the ref (could be null).
     *
     * @return the old value.
     */
    E clear();

    /**
     * Checks if the ref is null.
     *
     * @return true if the ref is null, false otherwise.
     */
    boolean isNull();
}

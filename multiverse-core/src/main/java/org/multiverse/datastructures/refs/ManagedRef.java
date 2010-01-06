package org.multiverse.datastructures.refs;

/**
 * An object responsible for storing an ref so that it can be managed by a
 * {@link org.multiverse.api.Transaction}.
 *
 * Atm there is no support for primitives, but if needed please send in a request
 * or create one yourself (just replace the generic type by a primitive version).
 *
 * @author Peter Veentjer
 */
public interface ManagedRef<E> {

    /**
     * Gets the current stored ref, or null if no ref is stored.
     *
     * @return the current stored ref, or null if no ref is stored.
     */
    E get();

    /**
     * Gets the current stored ref, or retries. So you will always get
     * a non null ref.
     *
     * @return the current stored reference.
     */
    E getOrAwait();

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

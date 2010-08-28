package org.multiverse.api.functions;

/**
 * A Function that accepts an argument of a certain type and returns a new value of the same type.
 *
 * Can be used for commuting functions or for the BetaRef.alter.
 *
 * @param <E>
 */
public interface Function<E> {

    /**
     * Calls the function
     *
     * @param arg the argument to call the function with.
     *
     * @return the new value.
     */
    E call(E arg);
}

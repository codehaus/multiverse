package org.multiverse.api.functions;

/**
 * A Function that accepts an argument of a certain type and returns a new value of the same type.
 * <p/>
 * Can be used for commuting functions or for the Ref.alter methods.
 *
 * @author Peter Veentjer.
 */
public interface Function<E>{

    /**
     * Evaluates the function.
     *
     * @param value the value to evaluate.
     * @return the result of the evaluation
     */
    E call(E value);
}

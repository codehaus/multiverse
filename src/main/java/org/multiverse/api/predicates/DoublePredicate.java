package org.multiverse.api.predicates;

/**
 * A predicate that checks if some value leads to true or false.
 *
 * @author Peter Veentjer.
 */
public interface DoublePredicate{

    /**
     * Evaluates the predicate.
     *
     * @param value the value to evaluate.
     * @return true or false.
     */
    boolean evaluate(double value);
}
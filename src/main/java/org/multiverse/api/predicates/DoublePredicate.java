package org.multiverse.api.predicates;

/**
 * A predicate that checks if some value leads to true or false.
 *
 * @author Peter Veentjer.
 */
public abstract class DoublePredicate implements Predicate<Double>{

    /**
     * Evaluates the predicate
     *
     * @param current the current value.
     * @return true or false.
     */
    public abstract boolean evaluate(double current);

    @Override
    public final boolean evaluate(Double arg) {
        return evaluate((double) arg);
    }
}

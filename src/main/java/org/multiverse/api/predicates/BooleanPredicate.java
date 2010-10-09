package org.multiverse.api.predicates;

/**
 * A predicate that checks if some value leads to true or false.
 *
 * @author Peter Veentjer.
 */
public abstract class BooleanPredicate implements Predicate<Boolean> {

    /**
     * Evaluates the predicate
     *
     * @param current the current value.
     * @return true or false.
     */
    public abstract boolean evaluate(boolean current);

    @Override
    public final boolean evaluate(Boolean arg) {
        return evaluate((boolean) arg);
    }
}

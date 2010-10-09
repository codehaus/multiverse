package org.multiverse.api.predicates;

/**
 * A predicate that checks if some value leads to true or false.
 *
 * @author Peter Veentjer.
 */
public abstract class IntPredicate implements Predicate<Integer>{

    /**
     * Evaluates the predicate
     *
     * @param current the current value.
     * @return true or false.
     */
    public abstract boolean evaluate(int current);

    @Override
    public final boolean evaluate(Integer arg) {
        return evaluate((int) arg);
    }
}

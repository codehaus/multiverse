package org.multiverse.api.predicates;

/**
 * A predicate that checks if some value leads to true or false.
 *
 * @author Peter Veentjer.
 */
public abstract class LongPredicate implements Predicate<Long>{

    /**
     * Evaluates the predicate
     *
     * @param current the current value.
     * @return true or false.
     */
    public abstract boolean evaluate(long current);

    @Override
    public final boolean evaluate(Long arg) {
        return evaluate((long) arg);
    }
}

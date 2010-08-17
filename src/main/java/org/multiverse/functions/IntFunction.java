package org.multiverse.functions;

/**
 * A {@link Function} optimized for booleans. It depends on the stm if {@link #call(int)} without boxing or
 * {@link #call(Integer)} is done (with boxing).
 *
 * @author Peter Veentjer
 */
public abstract class IntFunction implements Function<Integer> {

    /**
     * Calculates the new value based on the current value.
     *
     * @param current the current value.
     * @return the new value.
     */
    public abstract int call(int current);

    @Override
    public final Integer call(Integer arg) {
        return call((int) arg);
    }
}

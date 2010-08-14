package org.multiverse.functions;

/**
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

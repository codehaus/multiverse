package org.multiverse.api.functions;

/**
 * A Function that accepts an argument of a certain type and returns a new value of the same type.
 * <p/>
 * Can be used for commuting functions or for the BetaRef.alter.
 *
 * @author Peter Veentjer.
 */
public abstract class IntFunction implements Function<Integer>{

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

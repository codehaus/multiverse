package org.multiverse.api.functions;

/**
 * A Function that accepts an argument of a certain type and returns a new value of the same type.
 * <p/>
 * Can be used for commuting functions or for the BetaRef.alter.
 *
 * @author Peter Veentjer.
 */
public abstract class BooleanFunction implements Function<Boolean>{

    /**
     * Calculates the new value based on the current value.
     *
     * @param current the current value.
     * @return the new value.
     */
    public abstract boolean call(boolean current);

    @Override
    public final Boolean call(Boolean arg) {
        return call((boolean) arg);
    }
}

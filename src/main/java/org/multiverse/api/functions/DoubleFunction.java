package org.multiverse.api.functions;

/**
 * A Function that accepts an argument of a certain type and returns a new value of the same type.
 * <p/>
 * Can be used for commuting functions or for the Ref.alter methods.
 *
 * @author Peter Veentjer.
 */
public abstract class DoubleFunction implements Function<Double>{

    /**
     * Calculates the new value based on the current value.
     *
     * @param current the current value.
     * @return the new value.
     */
    public abstract double call(double current);

    @Override
    public final Double call(Double arg) {
        return call((double) arg);
    }
}

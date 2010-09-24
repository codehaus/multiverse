package org.multiverse.api.functions;

/**
 * A {@link Function} optimized for a double. It depends on the stm if {@link #call(double)} without boxing or
 * {@link #call(Double)} is done (with boxing).
 *
 * @author Peter Veentjer
 */
public abstract class DoubleFunction implements Function<Double> {

    /**
     * Calculates the new value based on the current value.
     *
     * @param arg the current value
     * @return the new value.
     */
    public abstract double call(double arg);

    @Override
    public Double call(Double arg) {
        return call((double) arg);
    }
}

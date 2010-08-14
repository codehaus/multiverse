package org.multiverse.functions;

/**
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

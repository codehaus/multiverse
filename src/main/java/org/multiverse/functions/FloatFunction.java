package org.multiverse.functions;

/**
 * A {@link Function} optimized for booleans. It depends on the stm if {@link #call(float)} without boxing or
 * {@link #call(Float)} is done (with boxing).
 *
 * @author Peter Veentjer
 */
public abstract class FloatFunction implements Function<Float> {

    /**
     * Calculates the new value based on the current value.
     *
     * @param current the current value.
     * @return the new value.
     */
    abstract float call(float current);

    @Override
    public Float call(Float arg) {
        return call((float) arg);
    }
}

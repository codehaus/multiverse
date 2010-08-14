package org.multiverse.functions;

/**
 * @author Peter Veentjer
 */
public interface FloatFunction {

    /**
     * Calculates the new value based on the current value.
     *
     * @param current the current value.
     * @return the new value.
     */
    float call(float current);
}

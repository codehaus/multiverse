package org.multiverse.callables;

/**
 * @author Peter Veentjer
 */
public interface BooleanCallable {

    /**
     * Calculates the new value based on the current value.
     *
     * @param current the current value
     * @return the new value
     */
    boolean call(boolean current);
}

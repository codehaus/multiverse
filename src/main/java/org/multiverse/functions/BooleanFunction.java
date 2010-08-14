package org.multiverse.functions;

/**
 * @author Peter Veentjer
 */
public abstract class BooleanFunction implements Function<Boolean> {

    /**
     * Calculates the new value based on the current value.
     *
     * @param arg the current value
     * @return the new value
     */
    public abstract boolean call(boolean arg);

    @Override
    public final Boolean call(Boolean arg) {
        return call((boolean) arg);
    }
}

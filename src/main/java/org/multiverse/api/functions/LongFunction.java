package org.multiverse.api.functions;

/**
 * A Function that accepts an argument of a certain type and returns a new value of the same type.
 * <p/>
 * Can be used for commuting functions or for the Ref.alter methods.
 *
 * @author Peter Veentjer.
 */
public abstract class LongFunction implements Function<Long> {

    /**
     * Calculates the new value based on the current value.
     *
     * @param current the current value.
     * @return the new value.
     */
    public abstract long call(long current);

    @Override
    public final Long call(Long arg) {
        return call((long) arg);
    }
}

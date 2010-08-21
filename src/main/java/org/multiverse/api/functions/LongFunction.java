package org.multiverse.api.functions;

/**
 * A {@link Function} optimized for a long. It depends on the stm if {@link #call(long)} without boxing or
 * {@link #call(Long)} is done (with boxing).
 *
 * @author Peter Veentjer
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

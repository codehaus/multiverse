package org.multiverse.functions;

/**
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

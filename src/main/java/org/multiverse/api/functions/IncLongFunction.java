package org.multiverse.api.functions;

/**
 * A {@link LongFunction} that increments the value with the specified amount.
 *
 * @author Peter Veentjer.
 */
public final class IncLongFunction extends LongFunction {

    public final static IncLongFunction INSTANCE_INC_ONE = new IncLongFunction();

    private final long inc;

    /**
     * Creates an IncLongFunction that increments with 1.
     */
    public IncLongFunction() {
        this(1);
    }

    /**
     * Creates an IncLongFunction that increments with the given getAndIncrement.
     *
     * @param inc the number to increment with.
     */
    public IncLongFunction(long inc) {
        this.inc = inc;
    }

    @Override
    public long call(long current) {
        return current + inc;
    }

    @Override
    public String toString() {
        return "IncLongFunction{" +
                "inc=" + inc +
                '}';
    }
}

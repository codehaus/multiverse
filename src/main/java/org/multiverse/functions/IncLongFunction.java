package org.multiverse.functions;

/**
 * A {@link LongFunction} that increments the value with the specified amount.
 *
 * @author Peter Veentjer.
 */
public class IncLongFunction extends LongFunction {

    public final static IncLongFunction INSTANCE = new IncLongFunction();

    private final long extra;

    public IncLongFunction() {
        this(1);
    }

    public IncLongFunction(long extra) {
        this.extra = extra;
    }

    @Override
    public long call(long current) {
        return current + 1;
    }

    @Override
    public String toString() {
        return "IncLongFunction{" +
                "extra=" + extra +
                '}';
    }
}

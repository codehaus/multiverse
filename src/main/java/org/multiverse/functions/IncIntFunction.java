package org.multiverse.functions;

/**
 * A {@link IntFunction} that increased the value with the supplied amount.
 *
 * @author Peter Veentjer.
 */
public final class IncIntFunction extends IntFunction {

    public final static IncIntFunction INSTANCE = new IncIntFunction();

    private final int extra;

    public IncIntFunction() {
        this(1);
    }

    public IncIntFunction(int extra) {
        this.extra = extra;
    }

    @Override
    public int call(int current) {
        return current + 1;
    }

    @Override
    public String toString() {
        return "IncIntFunction{" +
                "extra=" + extra +
                '}';
    }
}

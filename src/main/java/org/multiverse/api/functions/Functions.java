package org.multiverse.api.functions;

/**
 * Functions utility class.
 *
 * @author Peter Veentjer.
 */
public class Functions {

    private static final IntFunction incOneIntFunction = new IncIntFunction(1);

    private static final LongFunction incOneLongFunction = new IncLongFunction(1);

    private static final IntFunction decOneIntFunction = new IncIntFunction(-1);

    private static final LongFunction decOneLongFunction = new IncLongFunction(-1);

    /**
     * Creates a identity IntFunction (a function that returns its input value). You will receive an existing
     * instance.
     *
     * @return the create identity IntFunction.
     */
    public static IntFunction newIntIdentityFunction() {
        return identityIntFunction;
    }

    /**
     * Creates a identity LongFunction (a function that returns its input value). You will receive an existing
     * instance.
     *
     * @return the create identity LongFunction.
     */
    public static LongFunction newLongIdentityFunction() {
        return identityLongFunction;
    }

    /**
     * Creates a IntFunction that increments. For the -1, 0 and 1 you get an already existing instance.
     *
     * @param amount the value to increment with. A negative value does a decrement.
     * @return the create identity IntFunction.
     */
    public static IntFunction newIncIntFunction(int amount) {
        switch (amount) {
            case 0:
                return identityIntFunction;
            case 1:
                return incOneIntFunction;
            case -1:
                return decOneIntFunction;
            default:
                return new IncIntFunction(amount);
        }
    }

    /**
     * Creates a LongFunction that increments with the given amount. For the -1, 0 and 1 you
     * get an already existing instance.
     *
     * @param amount the value to increment with. A negative value does a decrement.
     * @return the create identity IntFunction.
     */
    public static LongFunction newIncLongFunction(long amount) {
        if (amount == 0) {
            return identityLongFunction;
        }

        if (amount == 1) {
            return incOneLongFunction;
        }

        if (amount == -1) {
            return decOneLongFunction;
        }

        return new IncLongFunction(amount);
    }

    private static final IntFunction identityIntFunction = new IntFunction() {
        @Override
        public int call(int current) {
            return current;
        }
    };

    private static final LongFunction identityLongFunction = new LongFunction() {
        @Override
        public long call(long current) {
            return current;
        }
    };

    private static class IncIntFunction extends IntFunction {
        private final int value;

        public IncIntFunction(int value) {
            this.value = value;
        }

        @Override
        public int call(int current) {
            return current + value;
        }

        @Override
        public String toString() {
            return "IncIntFunction{" +
                    "value=" + value +
                    '}';
        }
    }

    private static class IncLongFunction extends LongFunction {
        private final long value;

        public IncLongFunction(long value) {
            this.value = value;
        }

        @Override
        public long call(long current) {
            return current + value;
        }

        @Override
        public String toString() {
            return "IncIntFunction{" +
                    "value=" + value +
                    '}';
        }
    }

    private Functions() {
    }
}

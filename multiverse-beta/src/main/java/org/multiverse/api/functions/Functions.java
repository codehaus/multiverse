package org.multiverse.api.functions;

/**
 * A utility class for {@link Function} functionality.
 *
 * @author Peter Veentjer.
 */
public class Functions {

    private static final IntFunction incOneIntFunction = new IncIntFunction(1);

    private static final LongFunction incOneLongFunction = new IncLongFunction(1);

    private static final IntFunction decOneIntFunction = new IncIntFunction(-1);

    private static final LongFunction decOneLongFunction = new IncLongFunction(-1);

    /**
     * Returns an identity IntFunction (a function that returns its input value). The instance is cached.
     *
     * @return the identity IntFunction.
     */
    public static IntFunction newIdentityIntFunction() {
        return identityIntFunction;
    }

    /**
     * Returns an identity LongFunction (a function that returns its input value). The instance is cached.
     *
     * @return the identity LongFunction.
     */
    public static LongFunction newIdentityLongFunction() {
        return identityLongFunction;
    }

    /**
     * Returns an {@link IntFunction} that increments the input value by one. The instance is cached.
     *
     * @return the increment IntFunction.
     */
    public static IntFunction newIncIntFunction() {
        return incOneIntFunction;
    }

    /**
     * Returns an {@link IntFunction} that decrements the input value by one. The instance is cached.
     *
     * @return the decrease IntFunction.
     */
    public static IntFunction newDecIntFunction() {
        return decOneIntFunction;
    }

    /**
     * Returns a {@link LongFunction} that increments the input value by one. The instance is cached.
     *
     * @return the increment LongFunction.
     */
    public static LongFunction newIncLongFunction() {
        return incOneLongFunction;
    }

    /**
     * Returns a {@link LongFunction} that decrements the input value by one. The instance is cached.
     *
     * @return the decrement LongFunction.
     */
    public static LongFunction newDecLongFunction() {
        return decOneLongFunction;
    }


    /**
     * Returns a {@link IntFunction} that increments with the given amount. For the -1, 0 and 1
     * a cached instance is returned. In the other cases a new instance is created.
     *
     * @param amount the value to increment with. A negative value does a decrement.
     * @return the increment IntFunction.
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
     * Returns a {@link LongFunction} that increments with the given amount. For the -1, 0 and 1
     * a cached instance is returned. In the other cases a new instance is created.
     *
     * @param amount the value to increment with. A negative value does a decrement.
     * @return the increment LongFunction.
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

        @Override
        public String toString() {
            return "IdentityIntFunction";
        }
    };

    private static final LongFunction identityLongFunction = new LongFunction() {
        @Override
        public long call(long current) {
            return current;
        }

        @Override
        public String toString() {
            return "IdentityLongFunction";
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
            return "IncLongFunction{" +
                    "value=" + value +
                    '}';
        }
    }

    //we don't want instances.
    private Functions() {
    }
}

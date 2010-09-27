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

    public static IntFunction newIntIdentityFunction() {
        return identityIntFunction;
    }

    public static LongFunction newLongIdentityFunction() {
        return identityLongFunction;
    }

    public static IntFunction newIncIntFunction(int increment) {
        switch (increment) {
            case 0:
                return identityIntFunction;
            case 1:
                return incOneIntFunction;
            case -1:
                return decOneIntFunction;
            default:
                return new IncIntFunction(increment);
        }
    }

    public static LongFunction newIncLongFunction(long increment) {
        if (increment == 0) {
            return identityLongFunction;
        }

        if (increment == 1) {
            return incOneLongFunction;
        }

        if (increment == -1) {
            return decOneLongFunction;
        }

        return new IncLongFunction(increment);
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

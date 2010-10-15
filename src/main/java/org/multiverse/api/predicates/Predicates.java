package org.multiverse.api.predicates;

/**
 * Predicate utility class.
 *
 * @author Peter Veentjer.
 */
public class Predicates {

    private final static Predicate IsNullPredicate = new Predicate() {
        public boolean evaluate(Object value) {
            return value == null;
        }
    };

    private final static Predicate IsNotNullPredicate = new Predicate() {
        public boolean evaluate(Object value) {
            return value != null;
        }
    };

    public static LongPredicate newEqualsLongPredicate(final long value) {
        return new LongPredicate() {
            @Override
            public boolean evaluate(long current) {
                return current == value;
            }
        };
    }

    public static LongPredicate newNotEqualsLongPredicate(final long value) {
        return new LongPredicate() {
            @Override
            public boolean evaluate(long current) {
                return current != value;
            }
        };
    }

    public static LongPredicate newLargerThanLongPredicate(final long value) {
        return new LongPredicate() {
            @Override
            public boolean evaluate(long current) {
                return current > value;
            }
        };
    }

    public static LongPredicate newLargerThanOrEqualsLongPredicate(final long value) {
        return new LongPredicate() {
            @Override
            public boolean evaluate(long current) {
                return current >= value;
            }
        };
    }

    public static LongPredicate newSmallerThanLongPredicate(final long value) {
        return new LongPredicate() {
            @Override
            public boolean evaluate(long current) {
                return current < value;
            }
        };
    }

    public static LongPredicate newSmallerThanOrEqualsLongPredicate(final long value) {
        return new LongPredicate() {
            @Override
            public boolean evaluate(long current) {
                return current <= value;
            }
        };
    }

    /**
     * Creates a Predicate that checks if the passed object is null. You will get an existing instance.
     *
     * @return the Predicate.
     */
    public static <E> Predicate<E> newIsNullPredicate() {
        return IsNullPredicate;
    }

    /**
     * Creates a Predicate that checks if the passed object is not null. You will get an existing instance.
     *
     * @return the Predicate.
     */
    public static <E> Predicate<E> newIsNotNullPredicate() {
        return IsNotNullPredicate;
    }

    private Predicates() {
    }
}

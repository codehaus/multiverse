package org.multiverse.api.predicates;

/**
 * Predicate utility class.
 *
 * @author Peter Veentjer.
 */
public class Predicates {

    private final static Predicate IsNullPredicate = new Predicate() {
        public boolean evaluate(Object value) {
            return value != null;
        }
    };

    private final static Predicate IsNotNullPredicate = new Predicate() {
        public boolean evaluate(Object value) {
            return value != null;
        }
    };

    public static Predicate newIsNullPredicate() {
        return IsNullPredicate;
    }

    public static Predicate newIsNotNullPredicate() {
        return IsNotNullPredicate;
    }

    private Predicates() {
    }
}

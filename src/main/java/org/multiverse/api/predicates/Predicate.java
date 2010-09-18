package org.multiverse.api.predicates;

/**
 * A predicate that checks if some value leads to true or false.
 *
 * @author Peter Veentjer.
 */
public interface Predicate<E>{

    Predicate IS_NOT_NULL_PREDICATE = new Predicate(){
        public boolean evaluate(Object value){
            return value != null;
        }
    };

    Predicate IS_NULL_PREDICATE = new Predicate(){
        public boolean evaluate(Object value){
            return value == null;
        }
    };

    /**
     * Evaluates the predicate.
     *
     * @param value the value to evaluate.
     * @return true or false.
     */
    boolean evaluate(E value);
}
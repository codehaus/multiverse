package org.multiverse.callables;

/**
 * @author Peter Veentjer
 */
public interface ObjectCallable<E> {

    /**
     * Calculates the new value based on the current value.
     *
     * @param current the current value.
     * @return the new value.
     */
    E call(E current);
}

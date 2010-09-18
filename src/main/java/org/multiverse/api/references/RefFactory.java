package org.multiverse.api.references;

/**
 * A Factory for creating references.
 *
 * @author Peter Veentjer.
 */
public interface RefFactory {

    /**
     * Creates a committed DoubleRef.
     *
     * @param value the initial value.
     * @return the created DoubleRef.
     */
    DoubleRef newDoubleRef(double value);

    /**
     * Creates a committed BooleanRef.
     *
     * @param value the initial value.
     * @return the created BooleanRef.
     */
    BooleanRef newBooleanRef(boolean value);

    /**
     * Creates a committed BetaIntRef.
     *
     * @param value the initial value.
     * @return the created BetaIntRef.
     */
    IntRef newIntRef(int value);

    /**
     * Creates a committed BetaLongRef.
     *
     * @param value the initial value.
     * @return the created BetaLongRef.
     */
    LongRef newLongRef(long value);

    /**
     * Creates a committed BetaRef.
     *
     * @param value the initial value
     * @param <E>   the type of the value.
     * @return the created BetaRef.
     */
    <E> Ref<E> newRef(E value);
}

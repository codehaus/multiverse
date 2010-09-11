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
     * Creates a IntRefArray.
     *
     * @param length the length of the array.
     * @return the created IntRefArray.
     * @throws IllegalArgumentException is size smaller than 0.
     */
    IntRefArray newIntRefArray(int length);

    /**
     * Creates a committed BetaLongRef.
     *
     * @param value the initial value.
     * @return the created BetaLongRef.
     */
    LongRef newLongRef(long value);

    /**
     * Creates a LongRefArray.
     *
     * @param length the length of the array.
     * @return the create LongRefArray.
     */
    LongRefArray newLongRefArray(int length);

    /**
     * Creates a committed BetaRef.
     *
     * @param value the initial value
     * @param <E>   the type of the value.
     * @return the created BetaRef.
     */
    <E> Ref<E> newRef(E value);

    /**
     * Creates a RefArray.
     *
     * @param length the array.
     * @param <E>    the type of the elements in the array.
     * @return the created RefArray.
     * @throws IllegalArgumentException if length smaller than 0.
     */
    <E> RefArray<E> newRefArray(int length);
}

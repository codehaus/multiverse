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
    DoubleRef createDoubleRef(double value);

    /**
     * Creates a committed BooleanRef.
     *
     * @param value the initial value.
     * @return the created BooleanRef.
     */
    BooleanRef createBooleanRef(boolean value);

    /**
     * Creates a committed BetaIntRef.
     *
     * @param value the initial value.
     * @return the created BetaIntRef.
     */
    IntRef createIntRef(int value);

    /**
     * Creates a IntRefArray.
     *
     * @param length the length of the array.
     * @return the created IntRefArray.
     * @throws IllegalArgumentException is size smaller than 0.
     */
    IntRefArray createIntRefArray(int length);

    /**
     * Creates a committed BetaLongRef.
     *
     * @param value the initial value.
     * @return the created BetaLongRef.
     */
    LongRef createLongRef(long value);

    /**
     * Creates a LongRefArray.
     *
     * @param length the length of the array.
     * @return the create LongRefArray.
     */
    LongRefArray createLongRefArray(int length);

    /**
     * Creates a committed BetaRef.
     *
     * @param value the initial value
     * @param <E>   the type of the value.
     * @return the created BetaRef.
     */
    <E> Ref<E> createRef(E value);

    /**
     * Creates a RefArray.
     *
     * @param length the array.
     * @param <E>    the type of the elements in the array.
     * @return the created RefArray.
     * @throws IllegalArgumentException if length smaller than 0.
     */
    <E> RefArray<E> createRefArray(int length);
}

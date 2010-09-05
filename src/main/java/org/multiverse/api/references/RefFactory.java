package org.multiverse.api.references;

/**
 * A Factory for creating references.
 *
 * @author Peter Veentjer.
 */
public interface RefFactory {

    /**
     * Creates a committed BetaIntRef.
     *
     * @param value the initial value.
     * @return the created BetaIntRef.
     */
    IntRef createIntRef(int value);

    /**
     * Creates a committed BetaLongRef.
     *
     * @param value the initial value.
     * @return the created BetaLongRef.
     */
    LongRef createLongRef(long value);

    /**
     * Creates a committed BetaRef.
     *
     * @param value the initial value
     * @param <E> the type of the value.
     * @return the created BetaRef.
     */
    <E> Ref<E> createRef(E value);
}

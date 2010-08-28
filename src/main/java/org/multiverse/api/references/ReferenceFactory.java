package org.multiverse.api.references;

import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;

/**
 * A Factory for creating references.
 *
 * @author Peter Veentjer.
 */
public interface ReferenceFactory {

    /**
     * Creates a committed BetaIntRef.
     *
     * @param value the initial value.
     * @return the created BetaIntRef.
     */
    BetaIntRef createIntRef(int value);

    /**
     * Creates a committed BetaLongRef.
     *
     * @param value the initial value.
     * @return the created BetaLongRef.
     */
    BetaLongRef createLongRef(long value);

    /**
     * Creates a committed BetaRef.
     *
     * @param value the initial value
     * @param <E> the type of the value.
     * @return the created BetaRef.
     */
    <E> BetaRef<E> createRef(E value);
}

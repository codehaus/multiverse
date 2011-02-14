package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.references.RefFactory;

/**
 * A {@link org.multiverse.api.references.RefFactory} tailored for the BetaStm.
 *
 * @author Peter Veentjer.
 */
public interface BetaRefFactory extends RefFactory {

    @Override
    BetaBooleanRef newBooleanRef(boolean value);

    @Override
    BetaDoubleRef newDoubleRef(double value);

    @Override
    BetaIntRef newIntRef(int value);

    @Override
    BetaLongRef newLongRef(long value);

    @Override
    <E> BetaRef<E> newRef(E value);
}

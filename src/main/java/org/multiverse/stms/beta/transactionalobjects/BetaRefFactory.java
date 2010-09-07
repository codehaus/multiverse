package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.references.RefFactory;

/**
 * A {@link org.multiverse.api.references.RefFactory} tailored for the BetaStm.
 *
 * @author Peter Veentjer.
 */
public interface BetaRefFactory extends RefFactory {

    @Override
    BetaBooleanRef createBooleanRef(boolean value);

    @Override
    BetaDoubleRef createDoubleRef(double value);

    @Override
    BetaIntRef createIntRef(int value);

    @Override
    BetaIntRefArray createIntRefArray(int length);

    @Override
    BetaLongRef createLongRef(long value);

    @Override
    BetaLongRefArray createLongRefArray(int length);

    @Override
    <E> BetaRef<E> createRef(E value);

    @Override
    <E> BetaRefArray<E> createRefArray(int length);
}

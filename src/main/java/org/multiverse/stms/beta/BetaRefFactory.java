package org.multiverse.stms.beta;

import org.multiverse.api.references.RefFactory;
import org.multiverse.stms.beta.transactionalobjects.*;

/**
 * A {@link org.multiverse.api.references.RefFactory} tailored for the BetaStm.
 *
 * @author Peter Veentjer.
 */
public interface BetaRefFactory extends RefFactory {

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

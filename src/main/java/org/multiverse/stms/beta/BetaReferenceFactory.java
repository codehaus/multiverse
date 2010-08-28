package org.multiverse.stms.beta;

import org.multiverse.api.references.ReferenceFactory;
import org.multiverse.stms.beta.transactionalobjects.BetaIntRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;

/**
 * A {@link org.multiverse.api.references.ReferenceFactory} tailored for the BetaStm.
 *
 * @author Peter Veentjer.
 */
public interface BetaReferenceFactory extends ReferenceFactory {

    BetaIntRef createIntRef(int value);

    BetaLongRef createLongRef(long value);

    <E> BetaRef<E> createRef(E value);
}

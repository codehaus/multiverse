package org.multiverse.stms.beta.transactionalobjects;

import org.multiverse.api.references.RefFactoryBuilder;

/**
 * A {@link org.multiverse.api.references.RefFactoryBuilder} tailored for the BetaStm.
 *
 * @author Peter Veentjer.
 */
public interface BetaRefFactoryBuilder extends RefFactoryBuilder {

    @Override
    BetaRefFactory build();
}

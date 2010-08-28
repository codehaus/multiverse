package org.multiverse.stms.beta;

import org.multiverse.api.references.ReferenceFactoryBuilder;

/**
 * A {@link org.multiverse.api.references.ReferenceFactoryBuilder} tailored for the BetaStm.
 * 
 * @author Peter Veentjer.
 */
public interface BetaReferenceFactoryBuilder extends ReferenceFactoryBuilder {

    @Override
    BetaReferenceFactory build();
}

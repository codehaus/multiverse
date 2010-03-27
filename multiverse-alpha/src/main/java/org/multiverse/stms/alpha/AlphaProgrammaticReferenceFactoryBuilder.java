package org.multiverse.stms.alpha;

import org.multiverse.api.ProgrammaticReferenceFactory;
import org.multiverse.api.ProgrammaticReferenceFactoryBuilder;

/**
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticReferenceFactoryBuilder
        implements ProgrammaticReferenceFactoryBuilder {

    @Override
    public ProgrammaticReferenceFactory build() {
        return new AlphaProgrammaticReferenceFactory();
    }
}

package org.multiverse.stms.alpha;

import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactoryBuilder;
import org.multiverse.utils.TodoException;

/**
 * A {@link ProgrammaticReferenceFactoryBuilder} specific for the {@link AlphaStm}.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticReferenceFactoryBuilder
        implements ProgrammaticReferenceFactoryBuilder {

    @Override
    public ProgrammaticReferenceFactoryBuilder withAbaDetection(boolean enabled) {
        throw new TodoException();
    }

    @Override
    public ProgrammaticReferenceFactoryBuilder withRetrySupport(boolean enabled) {
        throw new TodoException();
    }

    @Override
    public ProgrammaticReferenceFactory build() {
        return new AlphaProgrammaticReferenceFactory();
    }
}

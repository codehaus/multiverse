package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactoryBuilder;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.utils.TodoException;

/**
 * A {@link ProgrammaticReferenceFactoryBuilder} specific for the {@link org.multiverse.stms.alpha.AlphaStm}.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticReferenceFactoryBuilder
        implements ProgrammaticReferenceFactoryBuilder {

    private final AlphaStm stm;

    public AlphaProgrammaticReferenceFactoryBuilder(AlphaStm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
    }

    @Override
    public ProgrammaticReferenceFactoryBuilder withAbaDetection(boolean enabled) {
        throw new TodoException();
    }

    @Override
    public ProgrammaticReferenceFactoryBuilder withBlockingSupport(boolean enabled) {
        throw new TodoException();
    }

    @Override
    public ProgrammaticReferenceFactory build() {
        return new AlphaProgrammaticReferenceFactory(stm);
    }
}

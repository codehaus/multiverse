package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactoryBuilder;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * A {@link ProgrammaticReferenceFactoryBuilder} specific for the
 * {@link org.multiverse.stms.alpha.AlphaStm}.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticReferenceFactoryBuilder
        implements ProgrammaticReferenceFactoryBuilder {

    private final AlphaStm stm;

    /**
     * Creates a new AlphaProgrammaticReferenceFactoryBuilder.
     *
     * @param stm the AlphaStm this AlphaProgrammaticReferenceFactoryBuilder belongs to.
     * @throws NullPointerException if stm is null.
     */
    public AlphaProgrammaticReferenceFactoryBuilder(AlphaStm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
    }

    @Override
    public ProgrammaticReferenceFactory build() {
        return new AlphaProgrammaticReferenceFactory(stm);
    }
}

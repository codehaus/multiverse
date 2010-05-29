package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.api.programmatic.ProgrammaticRefFactoryBuilder;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * A {@link org.multiverse.api.programmatic.ProgrammaticRefFactoryBuilder} specific for the
 * {@link org.multiverse.stms.alpha.AlphaStm}.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticRefFactoryBuilder implements ProgrammaticRefFactoryBuilder {

    private final AlphaStm stm;

    /**
     * Creates a new AlphaProgrammaticRefFactoryBuilder.
     *
     * @param stm the AlphaStm this AlphaProgrammaticRefFactoryBuilder belongs to.
     * @throws NullPointerException if stm is null.
     */
    public AlphaProgrammaticRefFactoryBuilder(AlphaStm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
    }

    @Override
    public ProgrammaticRefFactory build() {
        return new AlphaProgrammaticRefFactory(stm);
    }
}

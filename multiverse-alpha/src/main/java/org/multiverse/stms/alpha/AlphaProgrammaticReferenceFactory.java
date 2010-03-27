package org.multiverse.stms.alpha;

import org.multiverse.api.ProgrammaticReference;
import org.multiverse.api.ProgrammaticReferenceFactory;
import org.multiverse.api.Transaction;

/**
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticReferenceFactory implements ProgrammaticReferenceFactory {

    @Override
    public <E> ProgrammaticReference<E> create(Transaction tx, E value) {
        return new AlphaProgrammaticReference<E>(tx, value);
    }

    @Override
    public <E> ProgrammaticReference<E> create(E value) {
        return new AlphaProgrammaticReference<E>(value);
    }
}

package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.Transaction;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReference;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.utils.TodoException;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticReferenceFactory implements ProgrammaticReferenceFactory {

    @Override
    public AlphaProgrammaticLong createLong(Transaction tx, long value) {
        return new AlphaProgrammaticLong((AlphaTransaction) tx, value);
    }

    @Override
    public AlphaProgrammaticLong createLong(long value) {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
        return new AlphaProgrammaticLong(tx, value);
    }

    @Override
    public ProgrammaticLong atomicCreateLong(long value) {
        throw new TodoException();
    }

    @Override
    public <E> AlphaProgrammaticReference<E> createReference(Transaction tx, E value) {
        return new AlphaProgrammaticReference<E>(tx, value);
    }

    @Override
    public <E> AlphaProgrammaticReference<E> createReference(E value) {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
        return new AlphaProgrammaticReference<E>(tx, value);
    }

    @Override
    public <E> ProgrammaticReference<E> atomicCreateReference(E value) {
        return new AlphaProgrammaticReference<E>((AlphaTransaction) null, value);
    }
}

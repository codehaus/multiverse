package org.multiverse.stms.alpha;

import org.multiverse.api.Transaction;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

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
    public <E> AlphaProgrammaticReference<E> create(Transaction tx, E value) {
        return new AlphaProgrammaticReference<E>(tx, value);
    }

    @Override
    public <E> AlphaProgrammaticReference<E> create(E value) {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
        return new AlphaProgrammaticReference<E>(tx, value);
    }
}

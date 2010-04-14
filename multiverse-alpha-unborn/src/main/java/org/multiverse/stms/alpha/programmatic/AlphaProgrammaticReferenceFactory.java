package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.Transaction;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReference;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * The {@link ProgrammaticReferenceFactory} implementation specific for
 * the AlphaStm.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticReferenceFactory
        implements ProgrammaticReferenceFactory {

    private final AlphaStm stm;

    /**
     * Creates a new AlphaProgrammaticReferenceFactory with the provided stm.
     *
     * @param stm this AlphaProgrammaticReference belongs to.
     * @throws NullPointerException if stm is null.
     */
    public AlphaProgrammaticReferenceFactory(AlphaStm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
    }

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
        return new AlphaProgrammaticLong(stm, value);
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

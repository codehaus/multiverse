package org.multiverse.stms.alpha.programmatic;

import org.multiverse.api.Transaction;
import org.multiverse.api.programmatic.ProgrammaticRefFactory;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * The {@link org.multiverse.api.programmatic.ProgrammaticRefFactory} implementation specific for
 * the AlphaStm.
 *
 * @author Peter Veentjer
 */
public final class AlphaProgrammaticRefFactory implements ProgrammaticRefFactory {

    private final AlphaStm stm;

    /**
     * Creates a new AlphaProgrammaticRefFactory with the provided stm.
     *
     * @param stm this AlphaProgrammaticRef belongs to.
     * @throws NullPointerException if stm is null.
     */
    public AlphaProgrammaticRefFactory(AlphaStm stm) {
        if (stm == null) {
            throw new NullPointerException();
        }
        this.stm = stm;
    }

    @Override
    public AlphaProgrammaticLongRef createLongRef(Transaction tx, long value) {
        return new AlphaProgrammaticLongRef((AlphaTransaction) tx, value);
    }

    @Override
    public AlphaProgrammaticLongRef createLongRef(long value) {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
        return new AlphaProgrammaticLongRef(tx, value);
    }

    @Override
    public AlphaProgrammaticLongRef atomicCreateLongRef(long value) {
        return new AlphaProgrammaticLongRef(stm, value);
    }

    @Override
    public <E> AlphaProgrammaticRef<E> createRef(Transaction tx, E value) {
        return new AlphaProgrammaticRef<E>(tx, value);
    }

    @Override
    public <E> AlphaProgrammaticRef<E> createRef(E value) {
        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
        return new AlphaProgrammaticRef<E>(tx, value);
    }

    @Override
    public <E> AlphaProgrammaticRef<E> atomicCreateRef(E value) {
        return new AlphaProgrammaticRef<E>((AlphaTransaction) null, value);
    }

    @Override
    public <E> AlphaProgrammaticRef<E> atomicCreateRef() {
        return atomicCreateRef((E)null);
    }
}

package org.multiverse.stms.beta.integrationtest.isolation.levels;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.IsolationLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionFactory;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.BetaStmTestUtils.newReadBiasedLongRef;

public class IsolationLevelReadCommittedTest {
    private BetaStm stm;
    private BetaTransactionFactory transactionFactory;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
        transactionFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setIsolationLevel(IsolationLevel.ReadCommitted)
                .build();
    }

    @Test
    public void repeatableRead_whenTracked_thenNoInconsistentRead() {
        final BetaLongRef ref = newLongRef(stm);

        transactionFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadTrackingEnabled(true)
                .setIsolationLevel(IsolationLevel.ReadCommitted)
                .build();

        BetaTransaction tx = transactionFactory.newTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        long read2 = ref.get(tx);
        assertEquals(0, read2);
    }

    @Test
    public void repeatableRead_whenNotTracked_thenInconsistentReadPossible() {
        final BetaLongRef ref = newReadBiasedLongRef(stm);

        transactionFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .setIsolationLevel(IsolationLevel.ReadCommitted)
                .build();

        BetaTransaction tx = transactionFactory.newTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        long read2 = ref.get(tx);
        assertEquals(1, read2);
    }

    @Test
    public void causalConsistency_whenConflictingUpdate_thenNotDetected() {
        final BetaLongRef ref1 = newLongRef(stm);
        final BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = transactionFactory.newTransaction();

        ref1.get(tx);

        stm.getDefaultAtomicBlock().execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                ref1.incrementAndGet(1);
                ref2.incrementAndGet(1);
            }
        });

        long result = ref2.get(tx);

        assertEquals(1, result);
    }

    @Test
    public void writeSkewPossible() {
        final BetaLongRef ref1 = newLongRef(stm);
        final BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = transactionFactory.newTransaction();
        ref1.get(tx);
        ref2.incrementAndGet(tx, 1);

        ref1.atomicIncrementAndGet(1);

        tx.commit();

        assertEquals(1, ref1.atomicGet());
        assertEquals(1, ref2.atomicGet());
    }
}

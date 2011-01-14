package org.multiverse.stms.gamma.integration.isolation.levels;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.IsolationLevel;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.makeReadBiased;

public class IsolationLevelReadCommittedTest {
    private GammaStm stm;
    private GammaTransactionFactory transactionFactory;

    @Before
    public void setUp() {
        stm = (GammaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
        transactionFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setIsolationLevel(IsolationLevel.ReadCommitted)
                .build();
    }

    @Test
    public void repeatableRead_whenTracked_thenNoInconsistentRead() {
        final GammaLongRef ref = new GammaLongRef(stm);

        transactionFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadTrackingEnabled(true)
                .setIsolationLevel(IsolationLevel.ReadCommitted)
                .build();

        GammaTransaction tx = transactionFactory.newTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        long read2 = ref.get(tx);
        assertEquals(0, read2);
    }

    @Test
    public void repeatableRead_whenNotTracked_thenInconsistentReadPossible() {
        final GammaLongRef ref = makeReadBiased(new GammaLongRef(stm));

        transactionFactory = stm.createTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .setIsolationLevel(IsolationLevel.ReadCommitted)
                .build();

        GammaTransaction tx = transactionFactory.newTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        long read2 = ref.get(tx);
        assertEquals(1, read2);
    }

    @Test
    public void causalConsistency_whenConflictingUpdate_thenNotDetected() {
        final GammaLongRef ref1 = new GammaLongRef(stm);
        final GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();

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
        final GammaLongRef ref1 = new GammaLongRef(stm);
        final GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        ref1.get(tx);
        ref2.incrementAndGet(tx, 1);

        ref1.atomicIncrementAndGet(1);

        tx.commit();

        assertEquals(1, ref1.atomicGet());
        assertEquals(1, ref2.atomicGet());
    }
}

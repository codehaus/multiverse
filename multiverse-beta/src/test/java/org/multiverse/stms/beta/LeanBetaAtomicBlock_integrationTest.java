package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class LeanBetaAtomicBlock_integrationTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void whenExecutedThenThreadLocalTransactionSet() {
        BetaTransactionFactory transactionFactory = stm.newTransactionFactoryBuilder().build();
        AtomicBlock block = new LeanBetaAtomicBlock(transactionFactory);
        block.execute(new AtomicVoidClosure() {
            @Override
            public void execute(Transaction tx) throws Exception {
                assertNotNull(tx);
                assertIsActive((BetaTransaction) tx);
                assertSame(tx, getThreadLocalTransaction());
            }
        });

        assertNull(getThreadLocalTransaction());
    }

}

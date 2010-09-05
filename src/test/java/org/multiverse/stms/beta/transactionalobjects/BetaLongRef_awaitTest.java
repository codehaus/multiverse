package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.TestUtils.assertActive;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class BetaLongRef_awaitTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm);

        try {
            ref.await(null, 10);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaLongRef ref = createLongRef(stm);
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.await(tx, 10);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm);
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.await(tx, 10);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenCommittedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = createLongRef(stm);
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.await(tx, 10);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertAborted(tx);
    }

    @Test
    public void whenBlockingNotAllowed_thenNoRetryPossibleException() {
        BetaLongRef ref = createLongRef(stm);
        BetaTransaction tx = stm.startDefaultTransaction();

        try {
            ref.await(tx, 10);
            fail();
        } catch (Retry expected) {
        }

        assertActive(tx);
    }
}

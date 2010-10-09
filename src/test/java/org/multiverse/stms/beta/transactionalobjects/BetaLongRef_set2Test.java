package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

/**
 * Tests {@link BetaLongRef#set(BetaTransaction, long)}.
 *
 * @author Peter Veentjer.
 */
public class BetaLongRef_set2Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNullTransaction() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        try {
            ref.set(null, 11);
            fail();
        } catch (NullPointerException expected) {
        }

        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.set(tx, 11);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.set(tx, 11);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenCommittedTransaction_thenCommittedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.set(tx, 11);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        long result = ref.set(tx, 20);
        tx.commit();

        assertEquals(20, result);
        assertVersionAndValue(ref, version + 1, 20);
    }

    @Test
    public void whenNormalTransactionMethodCalled() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        Transaction tx = new FatMonoBetaTransaction(stm);
        long result = ref.set(tx, 20);
        tx.commit();

        assertEquals(20, result);
        assertVersionAndValue(ref, version + 1, 20);
    }
}

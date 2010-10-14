package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public class BetaRef_awaitNull1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNull_thenReturnImmediately() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.awaitNull(tx);

        RefTranlocal tranlocal = (RefTranlocal) tx.get(ref);
        assertTrue(tranlocal.isReadonly());
        assertTranlocalHasNoLock(tranlocal);
        assertRefHasNoLocks(ref);

        tx.commit();

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, null);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenNotNull_thenRetryError() {
        String initialValue = "foo";
        BetaRef ref = newRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();

        try {
            ref.awaitNull(tx);
            fail();
        } catch (Retry expected) {
        }

        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        try {
            ref.awaitNull(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.awaitNull(tx);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.awaitNull(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenCommittedTransaction_thenDeadTransactionException() {
        BetaRef ref = newRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.awaitNull(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }
}

package org.multiverse.stms.gamma.transactionalobjects.gammaref;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaRef_awaitNull0Test {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNull_thenReturnImmediately() {
        GammaRef<String> ref = new GammaRef<String>(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.awaitNull();

        GammaRefTranlocal tranlocal = tx.locate(ref);
        assertTrue(tranlocal.isRead());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasNoLocks(ref);

        tx.commit();

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, null);
        assertRefHasNoLocks(ref);
    }


    @Test
    public void whenPrivatizedByOther_thenReadWriteConflict() {
        GammaRef<String> ref = new GammaRef<String>(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.awaitNull();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, null);
        assertRefHasExclusiveLock(ref, otherTx);
    }

    @Test
    public void whenEnsuredByOther_thenSuccess() {
        GammaRef<String> ref = new GammaRef<String>(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.newDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.newDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.awaitNull();

        GammaRefTranlocal tranlocal = tx.locate(ref);
        assertTrue(tranlocal.isRead());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, otherTx);

        tx.commit();

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, null);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenNotNull_thenWait() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newTransactionFactoryBuilder()
                .newTransactionFactory()
                .newTransaction();

        setThreadLocalTransaction(tx);

        try {
            ref.awaitNull();
            fail();
        } catch (RetryError expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenNoTransactionAvailable_thenTransactionRequiredException() {
        GammaRef<String> ref = new GammaRef<String>(stm);
        long initialVersion = ref.getVersion();

        try {
            ref.awaitNull();
            fail();
        } catch (TransactionRequiredException expected) {
        }

        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        GammaRef<String> ref = new GammaRef<String>(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.awaitNull();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        GammaRef<String> ref = new GammaRef<String>(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        tx.abort();
        setThreadLocalTransaction(tx);

        try {
            ref.awaitNull();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }

    @Test
    public void whenCommittedTransaction_thenDeadTransactionException() {
        GammaRef<String> ref = new GammaRef<String>(stm);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.newDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        try {
            ref.awaitNull();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, null);
    }
}

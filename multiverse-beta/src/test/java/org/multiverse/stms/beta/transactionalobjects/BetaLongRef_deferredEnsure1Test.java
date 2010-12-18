package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.LOCKMODE_COMMIT;
import static org.multiverse.stms.beta.BetaStmTestUtils.LOCKMODE_NONE;
import static org.multiverse.stms.beta.BetaStmTestUtils.LOCKMODE_UPDATE;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;

public class BetaLongRef_deferredEnsure1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenReadonlyAndConflictingWrite_thenCommitSucceeds() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.get(tx);
        ref.deferredEnsure(tx);

        ref.atomicIncrementAndGet(1);

        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertSurplus(0, ref);
    }

    @Test
    public void whenEnsuredBySelf() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, initialValue + 1);
        ref.ensure(tx);
        ref.deferredEnsure(tx);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertIsActive(tx);
        assertTrue(tranlocal.isConflictCheckNeeded());
        assertEquals(LOCKMODE_UPDATE, tranlocal.getLockMode());

        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertSurplus(0, ref);
    }

    @Test
    public void whenPrivatizedBySelf() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, initialValue + 1);
        ref.privatize(tx);
        ref.deferredEnsure(tx);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertIsActive(tx);
        assertTrue(tranlocal.isConflictCheckNeeded());
        assertRefHasCommitLock(ref, tx);
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());

        tx.commit();

        assertRefHasNoLocks(ref);
        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertSurplus(0, ref);
    }

    @Test
    public void whenEnsuredByOther() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, initialValue + 1);
        ref.deferredEnsure(tx);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertIsActive(tx);
        assertTrue(tranlocal.isConflictCheckNeeded());
        assertRefHasUpdateLock(ref, otherTx);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedByOther_thenDeferredEnsureFails() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.deferredEnsure(tx);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void state_whenNullTransaction_thenNullPointerException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.deferredEnsure(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void state_whenAlreadyPrepared_thenPreparedTransactionException() {
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.deferredEnsure(tx);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void state_whenAlreadyAborted_thenDeadTransactionException() {
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.deferredEnsure(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void state_whenAlreadyCommitted_thenDeadTransactionException() {
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.deferredEnsure(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenPossibleWriteSkew_thenCanBeDetectedWithDeferredEnsure() {
        BetaLongRef ref1 = new BetaLongRef(stm);
        BetaLongRef ref2 = new BetaLongRef(stm);

        BetaTransaction tx1 = stm.startDefaultTransaction();
        ref1.get(tx1);
        ref2.incrementAndGet(tx1, 1);

        BetaTransaction tx2 = stm.startDefaultTransaction();
        ref1.incrementAndGet(tx2, 1);
        ref2.get(tx2);
        ref2.deferredEnsure(tx2);

        tx1.prepare();

        try {
            tx2.prepare();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx2);
    }

}
package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoCommitLock;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertHasNoUpdateLock;

public class BetaLongRef_deferredEnsure0Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void moreTesting() {
    }

    @Test
    public void whenCalled_thenNoLockingDuringTransaction() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.deferredEnsure();

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertTrue(tranlocal.isConflictCheckNeeded());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    @Ignore
    public void whenCalled_thenOnlySetDuringCommit(){

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

        assertSame(null, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(ref.___getLockOwner());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
    }

    @Test
    public void state_whenAlreadyPrepared_thenPreparedTransactionException() {
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.prepare();

        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.deferredEnsure();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(ref.___getLockOwner());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
    }

    @Test
    public void state_whenAlreadyAborted_thenDeadTransactionException() {
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.deferredEnsure();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(ref.___getLockOwner());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
    }

    @Test
    public void state_whenAlreadyCommitted_thenDeadTransactionException() {
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.deferredEnsure();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(ref.___getLockOwner());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
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

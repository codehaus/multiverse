package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class GammaLongRef_ensure0Test implements GammaConstants {
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenReadonlyAndConflictingWrite_thenCommitSucceeds(){
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.get();
        ref.ensure();

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
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(initialValue + 1);
        ref.getLock().acquire(LockMode.Write);
        ref.ensure();

        GammaTranlocal tranlocal = tx.get(ref);
        assertIsActive(tx);
        assertTrue(tranlocal.isConflictCheckNeeded());
        assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());

        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertSurplus(0, ref);
    }

    @Test
    public void whenPrivatizedBySelf() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(initialValue + 1);
        ref.getLock().acquire(LockMode.Commit);
        ref.ensure();

        GammaTranlocal tranlocal = tx.get(ref);
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
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(initialValue + 1);
        ref.ensure();

        GammaTranlocal tranlocal =  tx.get(ref);
        assertIsActive(tx);
        assertTrue(tranlocal.isConflictCheckNeeded());
        assertRefHasWriteLock(ref, otherTx);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedByOther_thenDeferredEnsureFails() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.ensure();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenCalled_thenNoLockingDuringTransaction() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure();

        GammaTranlocal tranlocal = tx.get(ref);

        assertTrue(tranlocal.isConflictCheckNeeded());
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void state_whenNullTransaction_thenNullPointerException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.ensure(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSame(null, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void state_whenAlreadyPrepared_thenPreparedTransactionException() {
        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.prepare();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.ensure();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void state_whenAlreadyAborted_thenDeadTransactionException() {
        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.ensure();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void state_whenAlreadyCommitted_thenDeadTransactionException() {
        GammaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.ensure();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenPossibleWriteSkew_thenCanBeDetectedWithDeferredEnsure() {
        GammaLongRef ref1 = new GammaLongRef(stm);
        GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx1 = stm.startDefaultTransaction();
        ref1.get(tx1);
        ref2.incrementAndGet(tx1, 1);

        GammaTransaction tx2 = stm.startDefaultTransaction();
        ref1.incrementAndGet(tx2, 1);
        ref2.get(tx2);
        ref2.ensure(tx2);

        tx1.prepare();

        try {
            tx2.prepare();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx2);
    }
}

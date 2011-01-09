package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class GammaTransaction_openForReadTest<T extends GammaTransaction> implements GammaConstants {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract int getMaxCapacity();

    protected abstract T newTransaction();

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    @Test
    public void whenOverflowing() {
        int maxCapacity = getMaxCapacity();
        assumeTrue(maxCapacity < Integer.MAX_VALUE);

        GammaTransaction tx = newTransaction();
        for (int k = 0; k < maxCapacity; k++) {
            GammaLongRef ref = new GammaLongRef(stm, 0);
            ref.openForRead(tx, LOCKMODE_NONE);
        }

        GammaLongRef ref = new GammaLongRef(stm, 0);
        try {
            ref.openForRead(tx, LOCKMODE_NONE);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
    }

    @Test
    public void whenReadonly() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();


        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.readonly = true;
        GammaTransaction tx = newTransaction(config);
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertEquals(initialVersion, tranlocal.version);
        assertEquals(initialValue, tranlocal.long_value);
        assertEquals(initialValue, tranlocal.long_oldValue);
        assertTrue(tranlocal.isRead());
        assertFalse(tx.hasWrites());
    }

    @Test
    public void whenNotOpenedBefore() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertEquals(initialVersion, tranlocal.version);
        assertEquals(initialValue, tranlocal.long_value);
        assertEquals(initialValue, tranlocal.long_oldValue);
        assertTrue(tranlocal.isRead());
        assertFalse(tx.hasWrites());
    }

    @Test
    public void whenRefAlreadyOpenedForRead() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        GammaTranlocal first = ref.openForRead(tx, LOCKMODE_NONE);
        GammaTranlocal second = ref.openForRead(tx, LOCKMODE_NONE);

        assertSame(first, second);
        assertNotNull(second);
        assertSame(ref, second.owner);
        assertEquals(initialVersion, second.version);
        assertEquals(initialValue, second.long_value);
        assertEquals(initialValue, second.long_oldValue);
        assertTrue(second.isRead());
        assertFalse(tx.hasWrites());
    }

    @Test
    public void whenRefAlreadyOpenedForWrite() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        GammaTranlocal first = ref.openForWrite(tx, LOCKMODE_NONE);
        GammaTranlocal second = ref.openForRead(tx, LOCKMODE_NONE);

        assertSame(first, second);
        assertNotNull(second);
        assertSame(ref, second.owner);
        assertEquals(initialVersion, second.version);
        assertEquals(initialValue, second.long_value);
        assertEquals(initialValue, second.long_oldValue);
        assertTrue(second.isWrite());
        assertTrue(tx.hasWrites());
    }

    // ====================== lock level ========================================



    // ===================== locking ============================================

    @Test
    public void locking_noLockRequired_whenLockedForReadByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_READ, tranlocal.getMode());
    }

    @Test
    public void locking_noLockRequired_whenLockedForWriteByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasWriteLock(ref, otherTx);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_READ, tranlocal.getMode());
    }

    @Test
    public void locking_noLockReqyired_whenLockedForCommitByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasCommitLock(ref, otherTx);
    }

    @Test
    public void locking_readLockRequired_whenFree() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);

        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 1);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_READ, tranlocal.getMode());
    }

    @Test
    public void locking_readLockRequired_whenLockedForReadByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);

        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, otherTx);
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 2);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_READ, tranlocal.getMode());
    }

    @Test
    public void locking_readLockRequired_whenLockedForWriteByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_READ);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void locking_readLockReqyired_whenLockedForCommitByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_READ);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasCommitLock(ref, otherTx);
    }

    @Test
    public void locking_writeLockRequired_whenFree() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_WRITE);

        assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasWriteLock(ref, tx);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_READ, tranlocal.getMode());
    }

    @Test
    public void locking_writeLockRequired_whenLockedForReadByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_WRITE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, otherTx);
    }

    @Test
    public void locking_writeLockRequired_whenLockedForWriteByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_WRITE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void locking_writeLockReqyired_whenLockedForCommitByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_WRITE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasCommitLock(ref, otherTx);
    }


    @Test
    public void locking_commitLockRequired_whenFree() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_COMMIT);

        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasCommitLock(ref, tx);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_READ, tranlocal.getMode());
    }

    @Test
    public void locking_commitLockRequired_whenLockedForReadByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_COMMIT);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
    }

    @Test
    public void locking_commitLockRequired_whenLockedForWriteByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx,LockMode.Write);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_COMMIT);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void locking_commitLockReqyired_whenLockedForCommitByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        T tx = newTransaction();
        try {
            ref.openForRead(tx, LOCKMODE_COMMIT);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasCommitLock(ref, otherTx);
    }

    // ================================================================

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        GammaTransaction tx = newTransaction();
        tx.prepare();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.openForRead(tx, LOCKMODE_NONE);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.version);
    }

    @Test
    public void whenTransactionAlreadyAborted_thenDeadTransactionException() {
        GammaTransaction tx = newTransaction();
        tx.abort();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();


        try {
            ref.openForRead(tx, LOCKMODE_NONE);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.version);
    }

    @Test
    public void whenTransactionAlreadyCommitted_thenDeadTransactionException() {
        GammaTransaction tx = newTransaction();
        tx.commit();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();


        try {
            ref.openForRead(tx, LOCKMODE_NONE);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.version);
    }
}

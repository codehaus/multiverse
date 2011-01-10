package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class GammaTransaction_openForWriteTest<T extends GammaTransaction> implements GammaConstants {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    protected abstract T newTransaction();

    protected abstract int getMaxCapacity();

    @Test
    public void whenOverflowing() {
        int maxCapacity = getMaxCapacity();
        assumeTrue(maxCapacity < Integer.MAX_VALUE);

        T tx = newTransaction();
        for (int k = 0; k < maxCapacity; k++) {
            GammaLongRef ref = new GammaLongRef(stm, 0);
            ref.openForWrite(tx, LOCKMODE_NONE);
        }

        GammaLongRef ref = new GammaLongRef(stm, 0);
        try {
            ref.openForWrite(tx, LOCKMODE_NONE);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
    }

    @Test
    public void whenReadonlyTransaction_thenReadonlyException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.readonly = true;
        T tx = newTransaction(config);

        try {
            ref.openForWrite(tx, LOCKMODE_NONE);
            fail();
        } catch (ReadonlyException expected) {

        }

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.version);
    }


    @Test
    public void whenNotOpenedBefore() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);

        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertEquals(initialVersion, tranlocal.version);
        assertEquals(initialValue, tranlocal.long_value);
        assertEquals(initialValue, tranlocal.long_oldValue);
        assertTrue(tranlocal.isWrite());
        assertTrue(tx.hasWrites());
    }

    @Test
    public void whenAlreadyOpenedForReading() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal first = ref.openForRead(tx, LOCKMODE_NONE);
        GammaTranlocal second = ref.openForWrite(tx, LOCKMODE_NONE);

        assertSame(first, second);
        assertSame(ref, second.owner);
        assertEquals(initialVersion, second.version);
        assertEquals(initialValue, second.long_value);
        assertEquals(initialValue, second.long_oldValue);
        assertTrue(second.isWrite());
        assertTrue(tx.hasWrites());
    }

    @Test
    public void whenAlreadyOpenedForWriting() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal first = ref.openForWrite(tx, LOCKMODE_NONE);
        GammaTranlocal second = ref.openForWrite(tx, LOCKMODE_NONE);

        assertSame(first, second);
        assertSame(ref, second.owner);
        assertEquals(initialVersion, second.version);
        assertEquals(initialValue, second.long_value);
        assertEquals(initialValue, second.long_oldValue);
        assertTrue(second.isWrite());
        assertTrue(tx.hasWrites());
    }

     @Test
    public void readConsistency_whenNotConsistent(){
         assumeTrue(getMaxCapacity()>1);

        GammaLongRef ref1 = new GammaLongRef(stm, 0);
        GammaLongRef ref2 = new GammaLongRef(stm, 0);

        GammaTransaction tx = newTransaction();
        ref1.openForWrite(tx, LOCKMODE_NONE);

        ref1.atomicIncrementAndGet(1);

        try{
        ref2.openForWrite(tx, LOCKMODE_NONE);
            fail();
        }catch(ReadWriteConflict expected){
        }

        assertIsAborted(tx);
    }

// ====================== lock level ========================================

    @Test
    public void lockLevel() {
        lockLevel(LOCKMODE_NONE, LOCKMODE_NONE, LOCKMODE_NONE);
        lockLevel(LOCKMODE_NONE, LOCKMODE_READ, LOCKMODE_READ);
        lockLevel(LOCKMODE_NONE, LOCKMODE_WRITE, LOCKMODE_WRITE);
        lockLevel(LOCKMODE_NONE, LOCKMODE_COMMIT, LOCKMODE_COMMIT);

        lockLevel(LOCKMODE_READ, LOCKMODE_NONE, LOCKMODE_READ);
        lockLevel(LOCKMODE_READ, LOCKMODE_READ, LOCKMODE_READ);
        lockLevel(LOCKMODE_READ, LOCKMODE_WRITE, LOCKMODE_WRITE);
        lockLevel(LOCKMODE_READ, LOCKMODE_COMMIT, LOCKMODE_COMMIT);

        lockLevel(LOCKMODE_WRITE, LOCKMODE_NONE, LOCKMODE_WRITE);
        lockLevel(LOCKMODE_WRITE, LOCKMODE_READ, LOCKMODE_WRITE);
        lockLevel(LOCKMODE_WRITE, LOCKMODE_WRITE, LOCKMODE_WRITE);
        lockLevel(LOCKMODE_WRITE, LOCKMODE_COMMIT, LOCKMODE_COMMIT);

        lockLevel(LOCKMODE_COMMIT, LOCKMODE_NONE, LOCKMODE_COMMIT);
        lockLevel(LOCKMODE_COMMIT, LOCKMODE_READ, LOCKMODE_COMMIT);
        lockLevel(LOCKMODE_COMMIT, LOCKMODE_WRITE, LOCKMODE_COMMIT);
        lockLevel(LOCKMODE_COMMIT, LOCKMODE_COMMIT, LOCKMODE_COMMIT);
    }

    public void lockLevel(int transactionWriteLockMode, int writeLockMode, int expectedLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.writeLockModeAsInt = transactionWriteLockMode;
        GammaTransaction tx = newTransaction(config);
        GammaTranlocal tranlocal = ref.openForWrite(tx, writeLockMode);

        assertEquals(expectedLockMode, tranlocal.getLockMode());
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasLockMode(ref, tx, expectedLockMode);
    }

    // ======================= lock upgrade ===================================

    @Test
    public void lockUpgrade() {
        lockUpgrade(LOCKMODE_NONE, LOCKMODE_NONE, LOCKMODE_NONE);
        lockUpgrade(LOCKMODE_NONE, LOCKMODE_READ, LOCKMODE_READ);
        lockUpgrade(LOCKMODE_NONE, LOCKMODE_WRITE, LOCKMODE_WRITE);
        lockUpgrade(LOCKMODE_NONE, LOCKMODE_COMMIT, LOCKMODE_COMMIT);

        lockUpgrade(LOCKMODE_READ, LOCKMODE_NONE, LOCKMODE_READ);
        lockUpgrade(LOCKMODE_READ, LOCKMODE_READ, LOCKMODE_READ);
        lockUpgrade(LOCKMODE_READ, LOCKMODE_WRITE, LOCKMODE_WRITE);
        lockUpgrade(LOCKMODE_READ, LOCKMODE_COMMIT, LOCKMODE_COMMIT);

        lockUpgrade(LOCKMODE_WRITE, LOCKMODE_NONE, LOCKMODE_WRITE);
        lockUpgrade(LOCKMODE_WRITE, LOCKMODE_READ, LOCKMODE_WRITE);
        lockUpgrade(LOCKMODE_WRITE, LOCKMODE_WRITE, LOCKMODE_WRITE);
        lockUpgrade(LOCKMODE_WRITE, LOCKMODE_COMMIT, LOCKMODE_COMMIT);

        lockUpgrade(LOCKMODE_COMMIT, LOCKMODE_NONE, LOCKMODE_COMMIT);
        lockUpgrade(LOCKMODE_COMMIT, LOCKMODE_READ, LOCKMODE_COMMIT);
        lockUpgrade(LOCKMODE_COMMIT, LOCKMODE_WRITE, LOCKMODE_COMMIT);
        lockUpgrade(LOCKMODE_COMMIT, LOCKMODE_COMMIT, LOCKMODE_COMMIT);
    }

    public void lockUpgrade(int firstMode, int secondLockMode, int expectedLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        ref.openForWrite(tx, firstMode);
        GammaTranlocal tranlocal = ref.openForWrite(tx, secondLockMode);

        assertEquals(expectedLockMode, tranlocal.getLockMode());
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasLockMode(ref, tx, expectedLockMode);
    }

    // ===================== locking ============================================

    @Test
    public void locking_noLockRequired_whenLockedForReadByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);

        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
    }

    @Test
    public void locking_noLockRequired_whenLockedForWriteByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);

        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasWriteLock(ref, otherTx);
        assertEquals(ref, tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
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
            ref.openForWrite(tx, LOCKMODE_NONE);
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
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_READ);

        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 1);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
    }

    @Test
    public void locking_readLockRequired_whenLockedForReadByOther() {
        long intitialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, intitialValue);
        long initialVersion = ref.getVersion();

        T otherTx = newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_READ);

        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 2);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
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
            ref.openForWrite(tx, LOCKMODE_READ);
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
            ref.openForWrite(tx, LOCKMODE_READ);
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
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_WRITE);

        assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasWriteLock(ref, tx);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
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
            ref.openForWrite(tx, LOCKMODE_WRITE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
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
            ref.openForWrite(tx, LOCKMODE_WRITE);
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
            ref.openForWrite(tx, LOCKMODE_WRITE);
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
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_COMMIT);

        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertVersionAndValue(ref, initialVersion, intitialValue);
        assertRefHasCommitLock(ref, tx);
        assertEquals(ref, tranlocal.owner);
        assertEquals(TRANLOCAL_WRITE, tranlocal.getMode());
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
            ref.openForWrite(tx, LOCKMODE_COMMIT);
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
        ref.getLock().acquire(otherTx, LockMode.Write);

        T tx = newTransaction();
        try {
            ref.openForWrite(tx, LOCKMODE_COMMIT);
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
            ref.openForWrite(tx, LOCKMODE_COMMIT);
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
            ref.openForWrite(tx, LOCKMODE_NONE);
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
            ref.openForWrite(tx, LOCKMODE_NONE);
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
            ref.openForWrite(tx, LOCKMODE_NONE);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.version);
    }
}

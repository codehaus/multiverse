package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class AbstractGammaObject_tryLockAndCheckConflictTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    // ===================== lock free ==================================

    @Test
    public void lockFree_tryNoneLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_NONE);

        assertTrue(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 0);
    }

    @Test
    public void lockFree_tryReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockFree_tryWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockFree_tryCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    // ==================== lock upgrade ========================

    @Test
    public void lockUpgrade_readLockAcquired_tryNoLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_NONE);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockUpgrade_readLockAcquired_tryReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockUpgrade_readLockAcquired_otherTransactionAlreadyAcquiredReadLock_tryReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 2);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 2);
    }

    @Test
    public void lockUpgrade_readLockAcquired_tryWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertTrue(result);

        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockUpgrade_readLockAcquired_otherTransactionAlsoAcquiredReadLock_tryWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertFalse(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 2);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 2);
    }

    @Test
    public void lockUpgrade_readLockAcquired_tryWriteCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockUpgrade_readLockAcquired_otherTransactionAlsoAcquiredReadLock_tryCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_READ);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertFalse(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 2);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 2);
    }

    @Test
    public void lockUpgrade_writeLockAcquired_tryNoLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_WRITE);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_NONE);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockUpgrade_writeLockAcquired_tryReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_WRITE);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockAcquired_writeLockAcquired_tryWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_WRITE);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_WRITE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockAcquired_writeLockAcquired_tryCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_WRITE);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockUpgrade_commitLockAcquired_tryNoLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_COMMIT);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_NONE);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockUpgrade_commitLockAcquired_tryReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_COMMIT);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockAcquired_commitLockAcquired_tryWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_COMMIT);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockAcquired_commitLockAcquired_tryCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_COMMIT);
        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_COMMIT, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    // ===================== lock free ==================================

    @Test
    public void lockFreeButConflictingUpdate_tryNoneLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_NONE);

        assertTrue(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertSurplus(ref, 0);
    }

    @Test
    public void lockFreeButConflictingUpdate__tryReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertSurplus(ref, 0);
    }

    @Test
    public void lockFreeButConflictingUpdate__tryWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertSurplus(ref, 0);
    }

    @Test
    public void lockFreeButConflictingUpdate__tryCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertSurplus(ref, 0);
    }

    // ===================== lock not free ==================================

    @Test
    public void lockNotFree_readLockAcquired_acquireNone() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_NONE);

        assertTrue(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_readLockAcquired_acquireReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertTrue(result);
        assertTrue(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_READ, tranlocal.getLockMode());
        assertRefHasReadLock(ref, tx);
        assertReadLockCount(ref, 2);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 2);
    }

    @Test
    public void lockNotFree_readLockAcquired_acquireWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_readLockAcquired_acquireCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_writeLockAcquired_acquireNoLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_NONE);

        assertTrue(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_writeLockAcquired_acquireReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_writeLockAcquired_acquireWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_writeLockAcquired_acquireCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasWriteLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_commitLockAcquired_acquireNoLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_NONE);

        assertTrue(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_commitLockAcquired_acquireReadLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_READ);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_commitLockAcquired_acquireWriteLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_WRITE);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }

    @Test
    public void lockNotFree_commitLockAcquired_acquireCommitLock() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        boolean result = ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_COMMIT);

        assertFalse(result);
        assertFalse(tranlocal.hasDepartObligation());
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 1);
    }
}

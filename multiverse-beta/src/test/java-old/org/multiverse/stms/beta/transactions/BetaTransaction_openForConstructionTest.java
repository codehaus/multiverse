package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockLevel;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.*;

public abstract class BetaTransaction_openForConstructionTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    protected abstract boolean hasLocalConflictCounter();

    protected abstract int getMaxTransactionCapacity();

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        BetaTransaction tx = newTransaction();

        try {
            tx.openForConstruction((BetaLongRef) null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenSuccess() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        long version = ref.getVersion();
        BetaLongRefTranlocal write = tx.openForConstruction(ref);

        assertNotNull(write);
        assertEquals(0, write.value);
        assertEquals(version, write.version);
        assertSame(ref, write.owner);
        assertFalse(write.isCommuting());
        assertFalse(write.hasDepartObligation());
        assertTrue(write.isDirty());

        assertIsActive(tx);
        assertAttached(tx, write);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
    }

    @Test
    public void whenAlreadyOpenedForConstruction_thenNoProblem() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal construction1 = tx.openForConstruction(ref);
        BetaLongRefTranlocal construction2 = tx.openForConstruction(ref);

        assertNotNull(construction1);
        assertSame(construction1, construction2);
        assertEquals(0, construction1.value);
        assertSame(ref, construction1.owner);
        assertFalse(construction1.isReadonly());
        assertFalse(construction1.hasDepartObligation());

        assertIsActive(tx);
        assertAttached(tx, construction1);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertTrue(construction1.isDirty());
    }

    @Test
    public void whenAlreadyCommitted_thenIllegalArgumentException() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 100);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForReadingAndPrivatized_thenIllegalArgumentException() {
        long initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        ref.get(tx);
        ref.getLock().acquire(tx, LockMode.Exclusive);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAlreadyOpenedForReadingAndEnsured_thenIllegalArgumentException() {
        long initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        ref.get(tx);
        ref.getLock().acquire(tx, LockMode.Write);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAlreadyPrivatizedByOther_thenIllegalArgumentException() {
        long initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        ref.set(tx, initialValue + 1);
        ref.getLock().acquire(tx, LockMode.Exclusive);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAlreadyEnsuredByOther_thenIllegalArgumentException() {
        long initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        BetaTransaction tx = newTransaction();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenAlreadyOpenedForWritingAndPrivatized_thenIllegalArgumentException() {
        long initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        BetaTransaction tx = newTransaction();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasCommitLock(ref, otherTx);
    }

    @Test
    public void whenAlreadyOpenedForWritingAndEnsured_thenIllegalArgumentException() {
        long initialValue = 100;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        ref.set(tx, initialValue + 1);
        ref.getLock().acquire(tx, LockMode.Write);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAlreadyOpenedForReading_thenIllegalArgumentException() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref, LOCKMODE_NONE);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 100);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenIllegalArgumentException() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 100);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    // ================= readonly =========================

    @Test
    public void whenReadonly_thenReadonlyException() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);

        BetaTransaction tx = newTransaction(config);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 0);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    // ================ isolation level ============================

    @Test
    public void lockLevel_whenPessimisticThenNoConflictDetectionNeeded() {
        assumeTrue(getMaxTransactionCapacity() > 2);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setLockLevel(LockLevel.CommitLockReads)
                .init();

        BetaTransaction tx = newTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        BetaLongRef ref2 = new BetaLongRef(tx);
        tx.openForConstruction(ref2);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
    }

    // ============ consistency ============================

    @Test
    public void consistency_conflictCounterIsNotReset() {
        assumeTrue(hasLocalConflictCounter());

        BetaTransaction tx = newTransaction();
        long oldConflictCount = tx.getLocalConflictCounter().get();
        BetaLongRef ref = new BetaLongRef(tx);

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        tx.openForConstruction(ref);

        assertEquals(oldConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    @Ignore
    public void consistency_whenThereIsConflict_thenItIsNotTriggered() {

    }

    // ============================ state ====================

    @Test
    public void state_whenPrepared_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.prepare();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void state_whenAborted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.abort();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void state_whenCommitted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.commit();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}

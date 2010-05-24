package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LockNotFreeWriteConflict;
import org.multiverse.api.exceptions.OptimisticLockFailedWriteConflict;
import org.multiverse.api.exceptions.WriteSkewConflict;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;

public class MapUpdateAlphaTransaction_commitTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MapUpdateAlphaTransaction createSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new MapUpdateAlphaTransaction(config);
    }

    public MapUpdateAlphaTransaction createSutTransactionWithWriteSkew(boolean allowed) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withWriteSkewAllowed(allowed);

        return new MapUpdateAlphaTransaction(config);
    }


    // ===================== lock related functionality =====================================

    @Test
    public void freshObjectIsNotLocked() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = ManualRef.createUncommitted();

        AlphaTransaction tx = createSutTransaction();
        ref1.inc(tx);
        AlphaTranlocal tranlocal2 = tx.openForConstruction(ref2);

        long version = stm.getVersion();

        ref1.resetLockInfo();
        ref2.resetLockInfo();
        tx.commit();

        assertEquals(version + 1, stm.getVersion());
        assertIsCommitted(tx);

        ref2.assertNoLockAcquired();
        ref1.assertLockAcquired();
        assertNull(ref1.___getLockOwner());
        assertNull(ref2.___getLockOwner());
    }

    @Test
    public void lockIsNotAcquiredOnReadonlyTransaction() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref);
        ref.resetLockInfo();
        tx.commit();

        ref.assertNoLocksReleased();
        ref.assertNoLockAcquired();
    }

    @Test
    public void lockIsNotAcquiredOnReadonlyObjectInAnUpdateTransaction() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForWrite(ref1);
        tx.openForRead(ref2);
        ref2.resetLockInfo();
        tx.commit();

        ref2.assertNoLocksReleased();
        ref2.assertNoLockAcquired();
    }

    @Test
    public void lockIsAcquiredOnNonDirtyObjectInAnUpdateTransaction() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForWrite(ref);
        ref.resetLockInfo();
        tx.commit();

        ref.assertNoLocksReleased();
        ref.assertNoLockAcquired();
    }

    @Test
    public void lockIsAcquiredOnDirtyObject() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;
        ref.resetLockInfo();
        tx.commit();

        ref.assertLockAcquired();
        ref.assertLockReleased();
    }

    @Test
    public void lockIsNotAcquiredOnFreshObject() {
        AlphaTransaction tx = createSutTransaction();
        ManualRef ref = new ManualRef(tx, 0);
        tx.openForWrite(ref);
        ref.resetLockInfo();
        tx.commit();

        ref.assertNoLocksReleased();
        ref.assertNoLockAcquired();
    }

    // ================== commit =============================

    @Test
    public void whenUnused() {
        long startVersion = stm.getVersion();

        AlphaTransaction tx = createSutTransaction();
        tx.commit();

        assertEquals(startVersion, stm.getVersion());
        assertIsCommitted(tx);
    }

    @Test
    public void whenReadonly() {
        ManualRef ref = new ManualRef(stm, 10);
        AlphaTranlocal expectedTranlocal = ref.___load();
        long version = stm.getVersion();

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref);
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(expectedTranlocal, ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenDirty() {
        ManualRef ref = new ManualRef(stm, 10);

        long startVersion = stm.getVersion();
        AlphaTransaction tx = createSutTransaction();
        ref.inc(tx);
        tx.commit();

        ManualRefTranlocal stored = (ManualRefTranlocal) ref.___load(stm.getVersion());
        assertIsCommitted(tx);
        assertEquals(startVersion + 1, stm.getVersion());
        assertEquals(11, stored.value);
        assertEquals(stm.getVersion(), stored.getWriteVersion());
        assertEquals(ref, stored.getTransactionalObject());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenCommutingWrites() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(1);

        AlphaTransaction tx = createSutTransaction();
        ref.commutingInc(tx, 1);
        AlphaTranlocal tranlocal = tx.openForCommutingWrite(ref);
        assertTrue(tranlocal.isCommuting());
        long version = stm.getVersion();
        tx.commit();

        assertNull(ref.___getLockOwner());
        assertNull(ref.___getListeners());
        assertIsCommitted(tx);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(2, ref.atomicGet());
        assertSame(tranlocal, ref.___load());
        assertTrue(tranlocal.isCommitted());
        assertEquals(version + 1, tranlocal.getWriteVersion());
        assertNull(tranlocal.getOrigin());
    }

    @Test
    public void whenOnlyOpenedForConstruction_thenVersionNotIncreased() {
        AlphaTransaction tx = createSutTransaction();

        long startVersion = stm.getVersion();
        ManualRef ref = new ManualRef(tx, 10);
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(startVersion, stm.getVersion());
        ManualRefTranlocal stored = (ManualRefTranlocal) ref.___load(stm.getVersion());
        assertEquals(10, stored.value);
        assertEquals(stm.getVersion(), stored.getWriteVersion());
        assertEquals(ref, stored.getTransactionalObject());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenNonDirty() {
        ManualRef ref = new ManualRef(stm, 10);
        AlphaTranlocal tranlocal = ref.___load();

        long startVersion = stm.getVersion();
        AlphaTransaction tx = createSutTransaction();
        tx.openForWrite(ref);
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(startVersion, stm.getVersion());
        assertSame(tranlocal, ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenWriteConflict_thenVersionTooOldWriteConflict() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx = createSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);

        ref.inc(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();
        long version = stm.getVersion();

        tranlocal.value++;
        try {
            tx.commit();
            fail();
        } catch (OptimisticLockFailedWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___load());
        assertEquals(version, stm.getVersion());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenLocked_thenCommitLockNotFreeWriteConflict() {
        ManualRef ref = new ManualRef(stm, 0);

        long version = stm.getVersion();

        AlphaTransaction tx = createSutTransaction();
        ref.inc(tx);

        Transaction otherOwner = mock(Transaction.class);
        ref.___tryLock(otherOwner);

        try {
            tx.commit();
            fail();
        } catch (LockNotFreeWriteConflict e) {
        }

        ref.___releaseLock(otherOwner);

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(0, ref.get(stm));
    }

    @Test
    public void whenCommitted_thenIgnore() {
        ManualRef ref = new ManualRef(stm, 1);

        AlphaTransaction tx = createSutTransaction();
        ref.inc(tx);
        tx.commit();

        ManualRefTranlocal tranlocal = (ManualRefTranlocal) ref.___load();

        long version = stm.getVersion();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(tranlocal, ref.___load());
        assertEquals(2, ref.get(stm));
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 1);

        AlphaTransaction tx = createSutTransaction();
        ref.inc(tx);
        tx.abort();

        long version = stm.getVersion();
        try {
            tx.commit();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(1, ref.get(stm));
    }

    // =================================================

    @Test
    public void complexScenario1() {
        ManualRef ref1 = new ManualRef(stm, 10);
        ManualRef ref2 = new ManualRef(stm, 20);

        long version = stm.getVersion();

        AlphaTransaction tx = createSutTransaction();
        ref1.inc(tx);
        ref2.get(tx);
        ManualRef ref3 = new ManualRef(tx, 30);
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(11, ref1.get(stm));
        assertEquals(20, ref2.get(stm));
        assertEquals(30, ref3.get(stm));
        assertEquals(version + 1, ref1.___load().getWriteVersion());
        assertEquals(version + 1, ref3.___load().getWriteVersion());
        assertEquals(version, ref2.___load().getWriteVersion());
    }

    @Test
    public void complexScenario2() {
        ManualRef ref1 = new ManualRef(stm, 1);
        ManualRef ref2 = new ManualRef(stm, 2);
        ManualRef ref3 = new ManualRef(stm, 3);

        long startVersion = stm.getVersion();

        AlphaTransaction tx = createSutTransaction();
        ref1.inc(tx);
        ref2.inc(tx);
        ref3.get(tx);
        ManualRef ref4 = new ManualRef(tx, 4);
        ref4.inc(tx);
        ManualRef ref5 = new ManualRef(tx, 5);
        ManualRef ref6 = new ManualRef(tx, 6);
        tx.commit();

        assertEquals(startVersion + 1, stm.getVersion());
        assertIsCommitted(tx);
        assertEquals(2, ref1.get(stm));
        assertEquals(3, ref2.get(stm));
        assertEquals(3, ref3.get(stm));
        assertEquals(5, ref4.get(stm));
        assertEquals(5, ref5.get(stm));
        assertEquals(6, ref6.get(stm));

        assertNull(ref1.___getLockOwner());
        assertNull(ref2.___getLockOwner());
        assertNull(ref3.___getLockOwner());
        assertNull(ref4.___getLockOwner());
        assertNull(ref5.___getLockOwner());
        assertNull(ref6.___getLockOwner());
    }

    @Test
    public void whenWriteSkewAllowed_thenCommit() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx1 = createSutTransactionWithWriteSkew(true);
        tx1.openForRead(ref1);
        ref2.inc(tx1);

        AlphaTransaction tx2 = createSutTransactionWithWriteSkew(true);
        tx2.openForRead(ref2);
        ref1.inc(tx2);

        tx1.commit();
        tx2.commit();

        assertEquals(1, ref1.get(stm));
        assertEquals(1, ref2.get(stm));
    }

    @Test
    public void whenWriteSkewDisallowed_theWriteSkewConflict() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx1 = createSutTransactionWithWriteSkew(false);
        tx1.openForRead(ref1);
        ref2.inc(tx1);

        AlphaTransaction tx2 = createSutTransactionWithWriteSkew(false);
        tx2.openForRead(ref2);
        ref1.inc(tx2);

        tx1.commit();

        try {
            tx2.commit();
            fail();
        } catch (WriteSkewConflict expected) {
        }

        assertEquals(0, ref1.get(stm));
        assertEquals(1, ref2.get(stm));
    }
}

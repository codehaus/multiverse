package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LockNotFreeReadConflict;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLongTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.alpha.AlphaTestUtils.startTrackingUpdateTransaction;

/**
 * @author Peter Veentjer
 */
public class MapUpdateAlphaTransaction_openForCommutingWriteTest {
    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapUpdateAlphaTransaction startSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    public void whenVersionMatches() {
        ManualRef ref = new ManualRef(stm, 0);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForWrite(ref);

        assertNotSame(committed, found);
        assertSame(committed, found.getOrigin());
        assertEquals(ref, found.getTransactionalObject());
        assertEquals(committed.value, found.value);
        assertTrue(found.isUncommitted());
        assertIsActive(tx);
    }

    @Test
    public void whenNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = startSutTransaction();

        try {
            tx.openForCommutingWrite(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void openForWriteDoesNotLockAtomicObjects() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.resetLockInfo();
        tx.openForWrite(ref);

        ref.assertNoLocksReleased();
        ref.assertNoLockAcquired();
    }

    /**
     * In the previous version multiverse, it was allowed to do a load of an atomicobject that was locked even the
     * version of the current content matches the version of the transaction. If the atomicobject didn't have any
     * primitives to other objects, this should be alright. But if an object does have dependencies, these dependencies
     * could escape before they are committed. For now this has been disallowed.
     */
    @Test
    public void whenLockedAndEqualVersion_thenLockNotFreeReadConflict() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);
        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (LockNotFreeReadConflict expected) {
        }

        assertSame(owner, ref.___getLockOwner());
        assertIsActive(tx);
    }

    @Test
    public void whenVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx1 = startSutTransaction();

        //conflicting update
        ref.inc(stm);

        try {
            tx1.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertIsActive(tx1);
    }

    @Test
    public void whenLocked_thenLockNotFreeReadConflict() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        stmConfig.clock.tick();

        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (LockNotFreeReadConflict expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenSameTranlocalReturned() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        AlphaProgrammaticLongTranlocal exected = (AlphaProgrammaticLongTranlocal) tx.openForWrite(ref);
        AlphaProgrammaticLongTranlocal found = (AlphaProgrammaticLongTranlocal) tx.openForWrite(ref);

        assertSame(exected, found);
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenItIsUpgradedToOpenForWrite() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 20);

        AlphaTransaction tx = startSutTransaction();
        AlphaProgrammaticLongTranlocal read1 = (AlphaProgrammaticLongTranlocal) tx.openForRead(ref);
        AlphaProgrammaticLongTranlocal read2 = (AlphaProgrammaticLongTranlocal) tx.openForCommutingWrite(ref);

        assertNotSame(read1, read2);
        assertSame(read2.getOrigin(), read1);
        assertTrue(read2.isUncommitted());
        assertSame(read1.getTransactionalObject(), read2.getTransactionalObject());
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenendForCommutingWrite_thenSameTranlocalReturned() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 20);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal read1 = tx.openForCommutingWrite(ref);
        AlphaTranlocal read2 = tx.openForCommutingWrite(ref);

        assertSame(read1, read2);
        assertTrue(read2.isUncommitted());
        assertTrue(read2.isCommuting());
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal openedForConstruction = tx.openForConstruction(ref);
        AlphaTranlocal found = tx.openForCommutingWrite(ref);

        assertSame(openedForConstruction, found);
        assertFalse(found.isCommitted());
        assertFalse(found.isCommuting());
        assertIsActive(tx);
    }

    @Test
    public void whenOpenedForWriteOnDifferentTransactions_thenDifferentTranlocalsAreReturned() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 1);

        AlphaTransaction tx1 = startTrackingUpdateTransaction(stm);
        AlphaTranlocal found1 = tx1.openForCommutingWrite(ref);

        AlphaTransaction tx2 = startTrackingUpdateTransaction(stm);
        AlphaTranlocal found2 = tx2.openForCommutingWrite(ref);

        assertNotSame(found1, found2);
    }

    @Test
    public void whenUncommitted_thenNewTranlocalReturned() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted();

        AlphaTransaction tx = startTrackingUpdateTransaction(stm);

        AlphaTranlocal tranlocal = tx.openForCommutingWrite(ref);
        assertFalse(tranlocal.isCommitted());
        assertTrue(tranlocal.isUncommitted());
        assertSame(ref, tranlocal.getTransactionalObject());
        assertNull(tranlocal.getOrigin());

        assertIsActive(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        long expectedVersion = stm.getVersion();
        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        AlphaProgrammaticLong ref = AlphaProgrammaticLong.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        long expectedVersion = stm.getVersion();
        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.prepare();

        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}

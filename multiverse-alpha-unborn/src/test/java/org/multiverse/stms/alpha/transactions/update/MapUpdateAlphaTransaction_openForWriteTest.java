package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.alpha.AlphaTestUtils.startTrackingUpdateTransaction;

/**
 * @author Peter Veentjer
 */
public class MapUpdateAlphaTransaction_openForWriteTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
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
            tx.openForWrite(null);
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

    @Test
    public void whenLockedAndVersionTooOld_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm, 1);

        //start the transaction to sets its readversion
        AlphaTransaction tx = startSutTransaction();

        //do an atomic and conflicting update
        ref.set(stm, 10);

        ManualRefTranlocal expectedTranlocal = (ManualRefTranlocal) ref.___load();

        //lock it
        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);

        //try to load it, it should fail because the version stored is newer than the
        //readversion is the transaction allows.
        long version = stm.getVersion();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict ex) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(expectedTranlocal, ref.___load());
    }

    @Test
    public void whenReadConflict_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        //conflicting write
        ref.inc(stm);
        try {
            tx.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

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
        ManualRef ref = new ManualRef(stm, 0);

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
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal exected = (ManualRefTranlocal) tx.openForWrite(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForWrite(ref);

        assertSame(exected, found);
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenItIsUpgradedToOpenForWrite() {
        ManualRef ref = new ManualRef(stm, 20);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal read1 = (ManualRefTranlocal) tx.openForRead(ref);
        ManualRefTranlocal read2 = (ManualRefTranlocal) tx.openForWrite(ref);

        assertNotSame(read1, read2);
        assertSame(read2.getOrigin(), read1);
        assertTrue(read2.isUncommitted());
        assertSame(read1.getTransactionalObject(), read2.getTransactionalObject());
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForCommutingWrite_thenFixated() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);
        AlphaTranlocal found = tx.openForWrite(ref);

        assertSame(openedForCommutingWrite, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenAlreadyOpenedForCommutingWriteAndLockedButVersionMatches() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        AlphaTranlocal found = tx.openForWrite(ref);

        assertSame(openedForCommutingWrite, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenAlreadyOpenedForCommutingWriteAndLockedAndVersionTooOld_thenOldVersionNotFoundReadConflict() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);

        ref.atomicInc(10);

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertEquals(stm.getVersion(), version);
        assertIsActive(tx);
        assertTrue(openedForCommutingWrite.isCommuting());
    }

    @Test
    public void whenOpenedForWriteOnDifferentTransactions_thenDifferentTranlocalsAreReturned() {
        ManualRef ref = new ManualRef(stm, 1);

        AlphaTransaction tx1 = startTrackingUpdateTransaction(stm);
        ManualRefTranlocal found1 = (ManualRefTranlocal) tx1.openForWrite(ref);

        AlphaTransaction tx2 = startTrackingUpdateTransaction(stm);
        ManualRefTranlocal found2 = (ManualRefTranlocal) tx2.openForWrite(ref);

        assertNotSame(found1, found2);
    }

    @Test
    public void whenReferenceUncommitted_thenUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startTrackingUpdateTransaction(stm);

        long version = stm.getVersion();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        long expectedVersion = stm.getVersion();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        long expectedVersion = stm.getVersion();
        try {
            tx.openForWrite(ref);
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
            tx.openForWrite(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}

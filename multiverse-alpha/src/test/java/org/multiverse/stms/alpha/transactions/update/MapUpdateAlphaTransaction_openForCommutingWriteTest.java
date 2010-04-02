package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LockNotFreeReadConflict;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.alpha.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

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
        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        UpdateAlphaTransactionConfiguration config = new UpdateAlphaTransactionConfiguration(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                speculativeConfig,
                stmConfig.maxRetryCount, true, true, true, true, true, true);
        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    @Ignore
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
    @Ignore
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
    @Ignore
    public void whenLockedAndEqualVersion_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm, 0);
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
    @Ignore
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
    @Ignore
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
    @Ignore
    public void whenAlreadyOpenedForWrite_thenSameTranlocalReturned() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal exected = (ManualRefTranlocal) tx.openForWrite(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForWrite(ref);

        assertSame(exected, found);
        assertIsActive(tx);
    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForRead_thenItIsUpgradedToOpenForWrite() {
        ManualRef ref = new ManualRef(stm, 20);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal read1 = (ManualRefTranlocal) tx.openForRead(ref);
        ManualRefTranlocal read2 = (ManualRefTranlocal) tx.openForCommutingWrite(ref);

        assertNotSame(read1, read2);
        assertSame(read2.getOrigin(), read1);
        assertTrue(read2.isUncommitted());
        assertSame(read1.getTransactionalObject(), read2.getTransactionalObject());
        assertIsActive(tx);
    }

    @Test
    @Ignore
    public void whenAlreadyOpenendForCommutingWrite_thenSameTranlocalReturned() {

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

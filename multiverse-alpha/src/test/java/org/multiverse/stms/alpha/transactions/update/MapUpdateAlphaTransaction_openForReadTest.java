package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.alpha.AlphaTestUtils.startTrackingUpdateTransaction;

/**
 * @author Peter Veentjer
 */
public class MapUpdateAlphaTransaction_openForReadTest {

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

    public MapUpdateAlphaTransaction startSutTransactionWithoutAutomaticReadTracking() {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withExplictRetryAllowed(false)
                .withAutomaticReadTrackingEnabled(false);

        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    public void whenExplicitRetryNotAllowed() {
        ManualRef ref = new ManualRef(stm);

        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withExplictRetryAllowed(false);

        AlphaTransaction tx = new MapUpdateAlphaTransaction(config);
        tx.openForRead(ref);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsActive(tx);
        assertFalse(latch.isOpen());
    }


    @Test
    public void whenAutomaticReadTrackingDisabled_openForReadIsNotTracked() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking();
        tx.openForRead(ref);

        Map attachedMap = (Map) getField(tx, "attachedMap");
        assertTrue(attachedMap.isEmpty());
    }

    @Test
    public void whenNotCommittedBefore_thenUncommitted() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();

        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
        } catch (UncommittedReadConflict o) {

        }

        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
        assertNull(ref.___load());
    }

    @Test
    public void whenOpenedForRead_noLockingIsDone() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.resetLockInfo();
        tx.openForRead(ref);

        ref.assertNoLocksReleased();
        ref.assertNoLockAcquired();
    }

    @Test
    public void whenTxObjectIsNull_thenNullReturned() {
        long expectedVersion = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();

        AlphaTranlocal result = tx.openForRead(null);

        assertNull(result);
        assertIsActive(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenOpenedForRead_thenReadonlyVersionIsReturned() {
        ManualRef ref = new ManualRef(stm, 10);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(committed, tranlocal);
        assertIsActive(tx);
    }

    @Test
    public void whenExactVersionMatch() {
        ManualRef ref = new ManualRef(stm, 0);

        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load(stm.getVersion());

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal opened = (ManualRefTranlocal) tx.openForWrite(ref);
        assertNotSame(committed, opened);
        assertEquals(ref, opened.getTransactionalObject());
        assertEquals(committed.value, opened.value);
        assertTrue(opened.isUncommitted());
        //version doesn't need to be checked since it is not defined for a non committed value
        assertIsActive(tx);
    }

    /**
     * In the previous version multiverse, it was allowed to do a load of an txobject that was locked even the version
     * of the current content matches the version of the transaction. If the atomicobject didn't have any primitives to
     * other objects, this should be alright. But if an object does have dependencies, these dependencies could escape
     * before they are committed. For now this has been disallowed.
     */
    @Test
    public void whenLockedAndVersionMatch_thenLockNotFreeReadConflict() {
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
    public void whenVersionIsTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx1 = startSutTransaction();

        ref.inc(stm);

        try {
            tx1.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict ex) {
        }

        assertIsActive(tx1);
    }

    @Test
    public void whenLocked_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm, 0);

        stmConfig.clock.tick();

        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);

        AlphaTransaction t = startTrackingUpdateTransaction(stm);
        try {
            t.openForWrite(ref);
            fail();
        } catch (LockNotFreeReadConflict ex) {
        }

        assertIsActive(t);
    }

    @Test
    public void openForReadDoesNotLockAtomicObjects() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.resetLockInfo();
        tx.openForRead(ref);

        ref.assertNoLocksReleased();
        ref.assertNoLockAcquired();
    }

    @Test
    public void whenMultipleTransactionsAreUsedAndNoChangesAreMade_thenSameValuesAreReturned() {
        ManualRef ref = new ManualRef(stm, 1);

        AlphaTransaction tx1 = startTrackingUpdateTransaction(stm);
        ManualRefTranlocal found1 = (ManualRefTranlocal) tx1.openForRead(ref);

        AlphaTransaction tx2 = startTrackingUpdateTransaction(stm);
        ManualRefTranlocal found2 = (ManualRefTranlocal) tx2.openForRead(ref);

        assertSame(found1, found2);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenSameTranlocalReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();

        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForRead(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(expected, found);
        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenUpdatableVersionIsReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTrackingUpdateTransaction(stm);
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForWrite(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(expected, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenAlreadyOpenedForCommutingWrite_thenItIsFixated() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);
        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(openedForCommutingWrite, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 0);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();
        long expectedVersion = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
        assertSame(committed, ref.___load());
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 0);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();
        long expectedVersion = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___load());
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

    public Map getReadWriteMap(AlphaTransaction tx) {
        return (Map) getField(tx, "readWriteMap");
    }
}

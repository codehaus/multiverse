package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class MapReadonlyAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapReadonlyAlphaTransaction startTransactionUnderTest() {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true);
        return new MapReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenOpenForRead_thenNotLockTxObjects() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();
        ref.resetLockInfo();
        tx.openForRead(ref);

        ref.assertNoLockAcquired();
        ref.assertNoLocksReleased();
    }

    @Test
    public void whenNotCommittedBefore_thenUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startTransactionUnderTest();
        try {
            tx.openForRead(ref);
            fail();
        } catch (UncommittedReadConflict expected) {
        }

        assertIsActive(tx);
    }


    @Test
    public void whenFirstTimeOpenedForRead_thenReturnReadonlyValue() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startTransactionUnderTest();
        AlphaTranlocal found = tx.openForRead(ref);

        assertTrue(found.isCommitted());
        assertFalse(found.isCommuting());
        assertSame(found, committed);
        assertIsActive(tx);
    }

    @Test
    public void whenSecondTimeLoadForRead_sameTranlocalIsReturned() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction tx = startTransactionUnderTest();
        AlphaTranlocal expected = tx.openForRead(ref);

        AlphaTranlocal found = tx.openForRead(ref);

        assertTrue(found.isCommitted());
        assertFalse(found.isCommuting());
        assertSame(expected, found);
        assertIsActive(tx);
    }


    @Test
    public void whenVersionTooOld_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();

        //conflicting write
        ref.inc(stm);
        long expectedVersion = stm.getVersion();

        try {
            tx.openForRead(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertIsActive(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenLocked_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm);
        long expectedVersion = stm.getVersion();
        ManualRefTranlocal expectedTranlocal = (ManualRefTranlocal) ref.___load();

        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startTransactionUnderTest();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LockNotFreeReadConflict ex) {
        }

        assertIsActive(tx);
        assertEquals(expectedVersion, stm.getVersion());
        assertEquals(expectedTranlocal, ref.___load());
    }

    @Test
    public void whenAlreadyOpenedForRead_thenReadWillNotObserveChangesByOthers() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();
        AlphaTranlocal expected = tx.openForRead(ref);

        //update in other transaction
        ref.inc(stm);

        AlphaTranlocal found = tx.openForRead(ref);
        assertSame(expected, found);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm, 10);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.prepare();

        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 10);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.commit();

        long expectedVersion = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 10);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.abort();

        long expectedVersion = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }
}

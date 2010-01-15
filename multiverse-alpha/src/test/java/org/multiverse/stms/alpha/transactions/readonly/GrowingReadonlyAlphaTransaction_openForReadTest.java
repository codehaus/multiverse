package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.LoadLockedException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.api.exceptions.LoadUncommittedException;
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
public class GrowingReadonlyAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public GrowingReadonlyAlphaTransaction startTransactionUnderTest() {
        GrowingReadonlyAlphaTransaction.Config config = new GrowingReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount,
                true);
        return new GrowingReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenOpenForRead_thenNotLockTxObjects() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();
        ref.resetLockInfo();
        tx.openForRead(ref);

        assertFalse(ref.isTryLockCalled());
    }

    @Test
    public void whenNotCommittedBefore_thenLoadUncommittedException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startTransactionUnderTest();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadUncommittedException expected) {
        }

        assertIsActive(tx);
    }


    @Test
    public void whenFirstTimeOpenedForRead_thenReturnReadonlyValue() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startTransactionUnderTest();
        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(found, committed);
        assertIsActive(tx);
    }

    @Test
    public void whenSecondTimeLoadForRead_sameTranlocalIsReturned() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction tx = startTransactionUnderTest();
        AlphaTranlocal expected = tx.openForRead(ref);

        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(expected, found);
        assertIsActive(tx);
    }


    @Test
    public void whenVersionTooOld_thenLoadTooOldVersionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest();

        //conflicting write
        ref.inc(stm);
        long expectedVersion = stm.getVersion();

        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadTooOldVersionException expected) {
        }

        assertIsActive(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenLocked_thenLoadLockedException() {
        ManualRef ref = new ManualRef(stm);
        long expectedVersion = stm.getVersion();
        ManualRefTranlocal expectedTranlocal = (ManualRefTranlocal) ref.___load();

        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startTransactionUnderTest();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadLockedException ex) {
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

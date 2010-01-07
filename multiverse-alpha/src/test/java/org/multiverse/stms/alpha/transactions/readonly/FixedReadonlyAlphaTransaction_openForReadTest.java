package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.LoadLockedException;
import org.multiverse.api.exceptions.LoadTooOldVersionException;
import org.multiverse.api.exceptions.TransactionTooSmallException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

public class FixedReadonlyAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public FixedReadonlyAlphaTransaction startTransactionUnderTest(int size) {
        optimalSize.set(size);

        FixedReadonlyAlphaTransaction.Config config = new FixedReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.restartBackoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount, true, optimalSize, size);

        return new FixedReadonlyAlphaTransaction(config, size);
    }

    @Test
    public void whenTxObjectNull_thenNullReturned() {
        AlphaTransaction tx = startTransactionUnderTest(10);
        AlphaTranlocal tranlocal = tx.openForRead(null);
        assertNull(tranlocal);
    }

    @Test
    public void whenFirstTimeOpenForRead_thenReadTracked() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startTransactionUnderTest(10);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(committed, found);
        assertEquals(1, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenSecondTimeOpenedForRead_thenNotAddedToAttachedSet() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest(10);
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForRead(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(expected, found);
        assertEquals(1, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenDifferentOpenForReadsAreDone() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.openForRead(ref1);
        assertEquals(1, getField(tx, "firstFreeIndex"));

        tx.openForRead(ref2);
        assertEquals(2, getField(tx, "firstFreeIndex"));

        tx.openForRead(ref3);
        assertEquals(3, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenLocked_thenLoadLockedFailure() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startTransactionUnderTest(10);

        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadLockedException expected) {
        }

        assertIsActive(tx);
        assertEquals(0, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenVersionTooNew_thenLoadTooOldVersionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest(10);

        //conflicting write
        ref.inc(stm);

        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadTooOldVersionException expected) {
        }

        assertIsActive(tx);
        assertEquals(0, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenMaximumCapacityExceeded() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);
        ManualRef ref4 = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest(3);
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.openForRead(ref3);

        try {
            tx.openForRead(ref4);
            fail();
        } catch (TransactionTooSmallException expected) {
        }
        assertEquals(5, optimalSize.get());
    }
}

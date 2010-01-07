package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertFreshTranlocal;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertUpdatableClone;

public class TinyUpdateAlphaTransaction_openForWriteTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public TinyUpdateAlphaTransaction startSutTransaction() {
        TinyUpdateAlphaTransaction.Config config = new TinyUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.restartBackoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount,
                stmConfig.commitLockPolicy, true, optimalSize, true, true, true, true);
        return new TinyUpdateAlphaTransaction(config);
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
    public void whenOpenForWriteFirstTime_thenAttached() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal committed = ref.___load();
        AlphaTransaction tx = startSutTransaction();

        AlphaTranlocal found = tx.openForWrite(ref);

        assertUpdatableClone(ref, committed, found);
        assertSame(found, getField(tx, "attached"));
    }

    @Test
    public void whenOpenForWriteOnFreshObject_thenAttached() {
        ManualRef ref = ManualRef.createUncommitted();
        AlphaTransaction tx = startSutTransaction();

        AlphaTranlocal tranlocal = tx.openForWrite(ref);

        assertFreshTranlocal(ref, tranlocal);
        assertSame(tranlocal, getField(tx, "attached"));
    }


    @Test
    public void whenVersionTooNew_thenLoadTooOldVersionException() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction tx = startSutTransaction();
        ref.inc(stm);

        try {
            tx.openForWrite(ref);
            fail();
        } catch (LoadTooOldVersionException expected) {
        }

        assertIsActive(tx);
        assertNull(getField(tx, "attached"));
    }

    @Test
    public void whenLocked_thenLoadLockedException() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction lockOwner = mock(AlphaTransaction.class);
        ref.___tryLock(lockOwner);

        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (LoadLockedException ex) {
        }

        assertIsActive(tx);
        assertNull(getField(tx, "attached"));
    }

    @Test
    public void whenOpenForWriteSecondTime_thenPreviousTranlocalReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal found1 = tx.openForWrite(ref);
        AlphaTranlocal found2 = tx.openForWrite(ref);
        assertSame(found1, found2);
    }

    @Test
    public void whenAkreadyOpenedForRead_thenUpgradedToOpenedForWrite() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal committed = ref.___load();

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);
        AlphaTranlocal found = tx.openForWrite(ref);

        assertUpdatableClone(ref, committed, found);
        assertSame(found, getField(tx, "attached"));
    }

    @Test
    public void whenAlreadyAnotherOpenForRead_thenTransactionTooSmallException() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref1);

        try {
            tx.openForWrite(ref2);
            fail();
        } catch (TransactionTooSmallException ex) {
        }

        assertEquals(2, optimalSize.get());
    }

    @Test
    public void whenAlreadyAnotherOpenForWrite_thenTransactionTooSmallException() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref1);

        try {
            tx.openForWrite(ref2);
            fail();
        } catch (TransactionTooSmallException ex) {
        }

        assertEquals(2, optimalSize.get());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        long version = stm.getVersion();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        long version = stm.getVersion();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
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


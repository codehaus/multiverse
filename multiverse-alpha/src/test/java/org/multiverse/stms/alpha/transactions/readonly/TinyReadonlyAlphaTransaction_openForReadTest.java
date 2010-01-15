package org.multiverse.stms.alpha.transactions.readonly;

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
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

public class TinyReadonlyAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createFastConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public TinyReadonlyAlphaTransaction startSutTransaction() {
        TinyReadonlyAlphaTransaction.Config config = new TinyReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount, true, optimalSize);
        return new TinyReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenSuccess() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(committed, found);
        assertSame(committed, getField(tx, "attached"));
    }

    @Test
    public void whenNotCommittedBefore_thenLoadUncommittedException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadUncommittedException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenLocked_thenLoadLockedException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadLockedException expected) {
        }

        assertIsActive(tx);
        assertNull(getField(tx, "attached"));
    }

    @Test
    public void whenAlreadyAttached_thenSameTranlocalReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal found1 = (ManualRefTranlocal) tx.openForRead(ref);
        ManualRefTranlocal found2 = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(found1, found2);
    }

    @Test
    public void whenReadConflict_thenLoadTooOldVersion() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.inc(stm);
        try {
            tx.openForRead(ref);
            fail();
        } catch (LoadTooOldVersionException expected) {
        }

        assertIsActive(tx);
        assertNull(getField(tx, "attached"));
    }

    @Test
    public void whenLoaded_thenNotSubjectToReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal tranlocal1 = tx.openForRead(ref);
        ref.inc(stm);
        AlphaTranlocal tranlocal2 = tx.openForRead(ref);
        assertSame(tranlocal1, tranlocal2);
    }

    @Test
    public void whenMaximumCapacityIsReached_thenTransactionTooSmallException() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref1);

        try {
            tx.openForRead(ref2);
            fail();
        } catch (TransactionTooSmallException expected) {
        }

        assertIsActive(tx);
        assertEquals(2, optimalSize.get());
    }

    @Test(expected = DeadTransactionException.class)
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        tx.openForRead(ref);
    }

    @Test(expected = DeadTransactionException.class)
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        tx.openForRead(ref);
    }
}

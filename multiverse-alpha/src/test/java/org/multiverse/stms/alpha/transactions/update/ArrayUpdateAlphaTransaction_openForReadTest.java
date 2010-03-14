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

public class ArrayUpdateAlphaTransaction_openForReadTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public AlphaTransaction startSutTransaction(int size) {
        optimalSize.set(size);
        ArrayUpdateAlphaTransaction.Config config = new ArrayUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                true,
                optimalSize,
                true, true, true, true, size
        );
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    public AlphaTransaction startSutTransactionWithoutAutomaticReadTracking(int size) {
        optimalSize.set(size);
        ArrayUpdateAlphaTransaction.Config config = new ArrayUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                true,
                optimalSize,
                true, true, true, false, size
        );
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenAutomaticReadTrackingDisabled_openForReadIsNotTracked() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking(10);
        tx.openForRead(ref);

        assertEquals(0, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenTxObjectNull_thenNullReturned() {
        AlphaTransaction tx = startSutTransaction(1);
        AlphaTranlocal found = tx.openForRead(null);
        assertNull(found);
    }

    @Test
    public void whenNotOpenedBefore_committedVersionReturned() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(committed, found);
        testIncomplete();
    }

    @Test
    public void whenNotCommittedBefore_thenFreshTranlocalReturned() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction(10);
        AlphaTranlocal found = tx.openForRead(ref);

        assertNotNull(found);
        assertSame(ref, found.getTransactionalObject());
        assertTrue(found.isUncommitted());
    }

    @Test
    public void whenLocked_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction(10);

        try {
            tx.openForRead(ref);
            fail();
        } catch (LockNotFreeReadConflict expected) {
        }

        assertIsActive(tx);
        assertSame(owner, ref.___getLockOwner());
        testIncomplete();
    }

    @Test
    public void whenReadConflict_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        ref.inc(stm);

        try {
            tx.openForRead(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertIsActive(tx);
        testIncomplete();
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenOpenedVersionReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForWrite(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(expected, found);
        testIncomplete();
    }

    @Test
    public void whenPreviouslyLoadedForRead_thenSameVersionReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForRead(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(expected, found);
        testIncomplete();
    }

    @Test
    public void whenMultipleDifferentOpenForReads() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRefTranlocal committed1 = (ManualRefTranlocal) ref1.___load();
        ManualRef ref2 = new ManualRef(stm);
        ManualRefTranlocal committed2 = (ManualRefTranlocal) ref2.___load();

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal found1 = (ManualRefTranlocal) tx.openForRead(ref1);
        ManualRefTranlocal found2 = (ManualRefTranlocal) tx.openForRead(ref2);

        assertSame(committed1, found1);
        assertSame(committed2, found2);
        testIncomplete();
    }

    @Test
    public void whenCapacityExceeded_thenTransactionTooSmallError() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(2);
        tx.openForWrite(ref1);
        tx.openForWrite(ref2);

        try {
            tx.openForWrite(ref3);
            fail();
        } catch (TransactionTooSmallError expected) {
        }

        assertSame(4, optimalSize.get());
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenNotSubjectToWriteConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForWrite(ref);

        //conflicting write
        ref.inc(stm);

        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(expected, found);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenNotSubjectToWriteConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForRead(ref);

        //conflicting write
        ref.inc(stm);

        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(expected, found);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(1);
        tx.abort();

        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(1);
        tx.commit();

        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(1);
        tx.prepare();

        try {
            tx.openForRead(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}

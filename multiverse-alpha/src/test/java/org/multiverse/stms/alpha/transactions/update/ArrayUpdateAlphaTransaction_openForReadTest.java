package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.*;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;

public class ArrayUpdateAlphaTransaction_openForReadTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public AlphaTransaction startSutTransaction(SpeculativeConfiguration speculativeConfig) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withSpeculativeConfiguration(speculativeConfig);

        return new ArrayUpdateAlphaTransaction(config, speculativeConfig.getMaximumArraySize());
    }

    public AlphaTransaction startSutTransactionWithoutAutomaticReadTracking() {
        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        UpdateConfiguration config = new UpdateConfiguration(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                speculativeConfig,
                stmConfig.maxRetryCount, true, false, true, true, true, true, false);

        return new ArrayUpdateAlphaTransaction(config, 100);
    }

    public AlphaTransaction startSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);

        return new ArrayUpdateAlphaTransaction(config, 100);
    }

    @Test
    public void whenAutomaticReadTrackingDisabled_openForReadIsNotTracked() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking();
        tx.openForRead(ref);

        assertEquals(0, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenTxObjectNull_thenNullReturned() {
        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal found = tx.openForRead(null);
        assertNull(found);
    }

    @Test
    public void whenNotOpenedBefore_committedVersionReturned() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertTrue(found.isCommitted());
        assertFalse(found.isCommuting());
        assertSame(committed, found);
        testIncomplete();
    }

    @Test
    public void whenNotCommittedBefore_thenUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();

        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (UncommittedReadConflict expected) {

        }

        assertNull(ref.___load());
        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
    }

    @Test
    public void whenLocked_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = startSutTransaction();

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

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForWrite(ref);
        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(expected, found);
        testIncomplete();
    }

    @Test
    public void whenAlreadyOpenedForCommutingWrite() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(stm, 0);

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);
        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(openedForCommutingWrite, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenPreviouslyLoadedForRead_thenSameVersionReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal found1 = (ManualRefTranlocal) tx.openForRead(ref1);
        ManualRefTranlocal found2 = (ManualRefTranlocal) tx.openForRead(ref2);

        assertSame(committed1, found1);
        assertSame(committed2, found2);
        testIncomplete();
    }

    @Test
    public void whenCapacityExceeded_thenSpeculativeConfigurationFailure() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);
        ManualRef ref4 = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(true, false, false, 3);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);
        tx.openForWrite(ref1);
        tx.openForWrite(ref2);
        tx.openForWrite(ref3);

        try {
            tx.openForWrite(ref4);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertIsActive(tx);
        assertEquals(5, speculativeConfig.getOptimalSize());
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForConstruction(ref);
        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(openedForCommutingWrite, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenNotSubjectToWriteConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForWrite(ref);

        //conflicting write
        ref.inc(stm);

        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(expected, found);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenNotSubjectToWriteConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal expected = (ManualRefTranlocal) tx.openForRead(ref);

        //conflicting write
        ref.inc(stm);

        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(expected, found);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
        tx.prepare();

        try {
            tx.openForRead(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}

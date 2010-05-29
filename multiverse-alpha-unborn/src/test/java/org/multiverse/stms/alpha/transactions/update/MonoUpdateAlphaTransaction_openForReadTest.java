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
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLongRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.*;

public class MonoUpdateAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MonoUpdateAlphaTransaction createSutTransaction(SpeculativeConfiguration speculativeConfig) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withMaxRetries(10)
                .withSpeculativeConfiguration(speculativeConfig);
        return new MonoUpdateAlphaTransaction(config);
    }

    public MonoUpdateAlphaTransaction createSutTransaction() {
        return createSutTransaction(new SpeculativeConfiguration(100));
    }

    public MonoUpdateAlphaTransaction createSutTransactionWithoutAutomaticReadTracking() {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withMaxRetries(10)
                .withExplictRetryAllowed(false)
                .withReadTrackingEnabled(false);

        return new MonoUpdateAlphaTransaction(config);
    }

    @Test
    public void testAutomaticReadTrackingProperty() {
        AlphaTransaction tx = createSutTransactionWithoutAutomaticReadTracking();
        assertFalse(tx.getConfiguration().isReadTrackingEnabled());

        tx = createSutTransaction();
        assertTrue(tx.getConfiguration().isReadTrackingEnabled());
    }

    @Test
    public void whenAutomaticReadTrackingDisabled_openForReadIsNotTracked() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransactionWithoutAutomaticReadTracking();
        tx.openForRead(ref);

        AlphaTranlocal tranlocal = (AlphaTranlocal) getField(tx, "attached");
        assertNull("attached field was not null", getField(tx, "attached"));
    }

    @Test
    public void whenTxObjectNull_thenNullReturned() {
        AlphaTransaction tx = createSutTransaction();
        tx.start();

        AlphaTranlocal tranlocal = tx.openForRead(null);

        assertNull(tranlocal);
        assertIsActive(tx);
    }

    @Test
    public void whenNotCommittedBefore_thenLoadUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = createSutTransaction();

        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (UncommittedReadConflict expected) {

        }

        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
        assertNull(ref.___load());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenLockedButExactVersionMatch_thenSuccess() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal readonly = ref.___load();

        AlphaTransaction owner = mock(AlphaTransaction.class);
        ref.___tryLock(owner);

        AlphaTransaction tx = createSutTransaction();

        AlphaTranlocal tranlocal = tx.openForRead(ref);

        assertIsActive(tx);
        assertSame(readonly, tranlocal);
    }


    @Test
    public void whenLockedAndVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm, 1);

        //start the transaction to sets its readversion
        AlphaTransaction tx = createSutTransaction();
        tx.start();

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
            tx.openForRead(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict ex) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(expectedTranlocal, ref.___load());
    }

    @Test
    public void whenLockedAndVersionTooOld_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm, 1);

        //lock it
        Transaction owner = mock(Transaction.class);
        ref.___tryLock(owner);


        stm.getClock().tick();

        //start the transaction to sets its readversion
        AlphaTransaction tx = createSutTransaction();

        ManualRefTranlocal expectedTranlocal = (ManualRefTranlocal) ref.___load();

        //try to load it, it should fail because the version stored is newer than the
        //readversion is the transaction allows.
        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LockNotFreeReadConflict ex) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertEquals(expectedTranlocal, ref.___load());
    }

    @Test
    public void whenOlderVersionExists_thenOlderVersionReturned() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal committed = ref.___load();
        stmConfig.clock.tick();
        AlphaTransaction tx = createSutTransaction();

        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(committed, found);
        assertSame(committed, getField(tx, "attached"));
    }

    @Test
    public void whenVersionMatch() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();
        long version = stm.getVersion();
        AlphaTransaction tx = createSutTransaction();

        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(committed, tranlocal);
        assertSame(committed, getField(tx, "attached"));
        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction tx = createSutTransaction();
        tx.start();
        //the 'conflicting' update
        ref.inc(stm);

        try {
            tx.openForWrite(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertIsActive(tx);
        assertNull(getField(tx, "attached"));
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenSameTranlocalReturned() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal tranlocal = tx.openForWrite(ref);
        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(tranlocal, found);
    }

    @Test
    public void whenAlreadyOpenedForCommutingWrite_thenPrematureFixation() {
        AlphaProgrammaticLongRef ref1 = new AlphaProgrammaticLongRef(stm, 0);

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal tranlocal = tx.openForCommutingWrite(ref1);
        AlphaTranlocal found = tx.openForRead(ref1);

        assertSame(tranlocal, found);
        assertFalse(found.isCommitted());
        assertFalse(found.isCommuting());
    }

    @Test
    public void whenAlreadyOpenedForCommutingWriteAndLockedButVersionMatches() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 0);

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(openedForCommutingWrite, found);
        assertFalse(found.isCommuting());
        assertFalse(found.isCommitted());
    }

    @Test
    public void whenAlreadyOpenedForCommutingWriteAndLockedAndVersionTooOld_thenOldVersionNotFoundReadConflict() {
        AlphaProgrammaticLongRef ref = new AlphaProgrammaticLongRef(stm, 0);

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal openedForCommutingWrite = tx.openForCommutingWrite(ref);

        ref.atomicInc(10);

        Transaction lockOwner = mock(Transaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        try {
            tx.openForRead(ref);
            fail();
        } catch (OldVersionNotFoundReadConflict expected) {
        }

        assertEquals(stm.getVersion(), version);
        assertIsActive(tx);
        assertTrue(openedForCommutingWrite.isCommuting());
    }

    @Test
    public void whenAlreadyOpenedForConstruction_thenSameTranlocalReturned() {
        ManualRef ref1 = ManualRef.createUncommitted();

        AlphaTransaction tx = createSutTransaction();
        AlphaTranlocal tranlocal = tx.openForConstruction(ref1);
        AlphaTranlocal found = tx.openForCommutingWrite(ref1);

        assertSame(tranlocal, found);
        assertFalse(tranlocal.isCommuting());
    }

    @Test
    public void whenMaximumCapacityExceeded_thenSpeculativeConfigurationFailure() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        AlphaTransaction tx = createSutTransaction(speculativeConfig);
        tx.openForRead(ref1);

        long version = stm.getVersion();
        try {
            tx.openForRead(ref2);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertIsActive(tx);
        assertEquals(2, speculativeConfig.getOptimalSize());
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.commit();

        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.abort();

        try {
            tx.openForRead(ref);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.prepare();

        try {
            tx.openForRead(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
    }
}

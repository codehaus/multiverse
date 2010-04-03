package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.*;
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

public class MonoUpdateAlphaTransaction_openForReadTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MonoUpdateAlphaTransaction startSutTransaction(SpeculativeConfiguration speculativeConfig) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withSpeculativeConfiguration(speculativeConfig);
        return new MonoUpdateAlphaTransaction(config);
    }

    public MonoUpdateAlphaTransaction startSutTransaction() {
        return startSutTransaction(new SpeculativeConfiguration(100));
    }

    public MonoUpdateAlphaTransaction startSutTransactionWithoutAutomaticReadTracking() {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withAutomaticReadTracking(false);

        return new MonoUpdateAlphaTransaction(config);
    }

    @Test
    public void testAutomaticReadTrackingProperty() {
        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking();
        assertFalse(tx.getConfiguration().automaticReadTracking());

        tx = startSutTransaction();
        assertTrue(tx.getConfiguration().automaticReadTracking());
    }

    @Test
    public void whenAutomaticReadTrackingDisabled_openForReadIsNotTracked() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking();
        tx.openForRead(ref);

        AlphaTranlocal tranlocal = (AlphaTranlocal) getField(tx, "attached");
        assertNull("attached field was not null", getField(tx, "attached"));
    }

    @Test
    public void whenTxObjectNull_thenNullReturned() {
        AlphaTransaction tx = startSutTransaction();

        AlphaTranlocal tranlocal = tx.openForRead(null);

        assertNull(tranlocal);
        assertIsActive(tx);
    }

    @Test
    public void whenNotCommittedBefore_thenLoadUncommittedReadConflict() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();

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
    public void whenLocked_thenLockNotFreeReadConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction lockOwner = mock(AlphaTransaction.class);
        ref.___tryLock(lockOwner);

        long version = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForRead(ref);
            fail();
        } catch (LockNotFreeReadConflict expected) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenOlderVersionExists_thenOlderVersionReturned() {
        ManualRef ref = new ManualRef(stm);
        AlphaTranlocal committed = ref.___load();
        stmConfig.clock.tick();
        AlphaTransaction tx = startSutTransaction();

        ManualRefTranlocal found = (ManualRefTranlocal) tx.openForRead(ref);
        assertSame(committed, found);
        assertSame(committed, getField(tx, "attached"));
    }

    @Test
    public void whenVersionMatch() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();
        long version = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();

        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForRead(ref);

        assertSame(committed, tranlocal);
        assertSame(committed, getField(tx, "attached"));
        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenVersionTooNew_thenOldVersionNotFoundReadConflict() {
        ManualRef ref = new ManualRef(stm);
        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
        AlphaTranlocal tranlocal = tx.openForWrite(ref);
        AlphaTranlocal found = tx.openForRead(ref);

        assertSame(tranlocal, found);
    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForCommutingWrite() {

    }

    @Test
    public void whenMaximumCapacityExceeded_thenSpeculativeConfigurationFailure() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);
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

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
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

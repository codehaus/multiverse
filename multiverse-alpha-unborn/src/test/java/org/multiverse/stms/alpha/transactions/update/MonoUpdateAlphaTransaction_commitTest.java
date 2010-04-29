package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.OptimisticLockFailedWriteConflict;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.programmatic.AlphaProgrammaticLong;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasNoListeners;

public class MonoUpdateAlphaTransaction_commitTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MonoUpdateAlphaTransaction startSutTransaction() {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withReadTrackingEnabled(true)
                .withExplictRetryAllowed(true);

        return new MonoUpdateAlphaTransaction(config);
    }

    @Test
    public void freshObjectIsNotLocked() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        tx.openForConstruction(ref);

        long version = stm.getVersion();

        ref.resetLockInfo();
        tx.commit();

        assertEquals(version, stm.getVersion());
        assertIsCommitted(tx);
        ref.assertNoLockAcquired();
        ref.assertNoLocksReleased();
    }

    @Test
    public void whenUnused_thenCommitSucceedsWithoutChange() {
        long startTime = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(startTime, stm.getVersion());
    }

    @Test
    public void whenReadonly_thenCommitSucceedsWithoutChange() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        long version = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
        assertNull(ref.___getListeners());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenUpdate_updatedTranlocalIsWritten() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        long version = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
        assertNull(ref.___getListeners());
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenDirty_newTranlocalIsWritten() {
        ManualRef ref = new ManualRef(stm);

        long version = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;

        assertNull(ref.___getLockOwner());
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version + 1, stm.getVersion());
        assertSame(tranlocal, ref.___load());
        assertNull(ref.___getListeners());
        assertTrue(tranlocal.isCommitted());
    }

    @Test
    public void whenCommutingWrites() {
        AlphaProgrammaticLong ref = new AlphaProgrammaticLong(1);

        AlphaTransaction tx = startSutTransaction();
        ref.commutingInc(tx, 1);
        AlphaTranlocal tranlocal = tx.openForCommutingWrite(ref);
        assertTrue(tranlocal.isCommuting());
        long version = stm.getVersion();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(2, ref.atomicGet());
        assertSame(tranlocal, ref.___load());
        assertTrue(tranlocal.isCommitted());
        assertEquals(version + 1, tranlocal.getWriteVersion());
        assertNull(tranlocal.getOrigin());
    }

    @Test
    public void whenFresh_commitSucceeds() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForConstruction(ref);
        long version = stm.getVersion();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(tranlocal, ref.___load());
        assertNull(ref.___getListeners());
        assertNull(ref.___getLockOwner());
        assertNull(tranlocal.getOrigin());
        assertEquals(stm.getVersion(), tranlocal.getWriteVersion());
    }

    @Test
    public void whenWriteConflict_thenVersionTooOldWriteConflict() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;

        //conflicting update
        ref.inc(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        long version = stm.getVersion();
        try {
            tx.commit();
            fail();
        } catch (OptimisticLockFailedWriteConflict ex) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenChange_listenersAreNotified() {
        ManualRef ref = new ManualRef(stm);

        Latch latch1 = new CheapLatch();
        Latch latch2 = new CheapLatch();

        registerRetryListener(ref, latch1);
        stmConfig.clock.tick();
        registerRetryListener(ref, latch2);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;
        tx.commit();

        assertHasNoListeners(ref);
        assertTrue(latch1.isOpen());
        assertTrue(latch2.isOpen());
    }

    private void registerRetryListener(ManualRef ref, Latch latch) {
        AlphaTransaction listenTx = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setReadTrackingEnabled(true)
                .setExplicitRetryAllowed(true)
                .build()
                .start();

        listenTx.openForRead(ref);
        listenTx.registerRetryLatch(latch);
    }

    @Test
    public void integrationTest() {
        ManualRef ref = new ManualRef(stm, 25);

        for (int k = 0; k < 100; k++) {
            AlphaTransaction tx = startSutTransaction();
            ref.inc(tx);
            tx.commit();
        }

        assertEquals(125, ref.get(stm));
    }
}

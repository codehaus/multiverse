package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasListeners;

public class TinyUpdateAlphaTransaction_abortTest {

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
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount,
                stmConfig.commitLockPolicy, true, optimalSize, true, true, true, true);
        return new TinyUpdateAlphaTransaction(config);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = startSutTransaction();
        long version = stm.getVersion();
        tx.abort();

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenReadonly() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();
        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);

        long version = stm.getVersion();
        tx.abort();
        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenUpdates() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);

        long version = stm.getVersion();
        tx.abort();
        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenDirty_changesAreDiscarded() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;

        long version = stm.getVersion();
        tx.abort();
        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenFresh_changesAreDiscarded() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);

        long version = stm.getVersion();
        tx.abort();

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
        assertNull(ref.___load());
    }

    @Test
    public void whenDirty_noListenersAreNotified() {
        ManualRef ref = new ManualRef(stm);

        Latch latch = new CheapLatch();

        AlphaTransaction listenTx = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setAutomaticReadTracking(true).build().start();
        listenTx.openForRead(ref);
        listenTx.registerRetryLatch(latch);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;
        tx.abort();

        assertIsAborted(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
    }

    @Test
    public void whenPreparedWithLockedResources_thenResourcesFreed() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.inc(tx);
        tx.prepare();

        tx.abort();
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
    }

}

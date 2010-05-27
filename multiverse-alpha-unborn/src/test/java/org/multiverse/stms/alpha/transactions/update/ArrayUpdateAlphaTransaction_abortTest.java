package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasListeners;

public class ArrayUpdateAlphaTransaction_abortTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createFastConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public AlphaTransaction createSutTransaction(int size) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock);
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = createSutTransaction(10);
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenReadsAreDone() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction(10);
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenWritesAreDone() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction(10);
        tx.openForWrite(ref1);
        tx.openForWrite(ref2);
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenPendingWrite_thenWriteDiscarded() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = createSutTransaction(10);
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;
        tx.abort();

        assertIsAborted(tx);
        assertSame(committed, ref.___load());
    }

    @Test
    public void whenListenersExists_theyAreNotNotified() {
        ManualRef ref = new ManualRef(stm);
        Latch latch = new CheapLatch();

        AlphaTransaction listenTx = stm.getTransactionFactoryBuilder()
                .setExplicitRetryAllowed(true)
                .setReadTrackingEnabled(true)
                .build()
                .start();
        listenTx.openForRead(ref);
        listenTx.registerRetryLatch(latch);

        AlphaTransaction tx = createSutTransaction(10);
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;
        tx.abort();

        assertHasListeners(ref, latch);
    }

    @Test
    public void whenPreparedWithLockedResources_thenResourcesFreed() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction(10);
        ref.inc(tx);
        tx.prepare();

        tx.abort();
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
    }

}

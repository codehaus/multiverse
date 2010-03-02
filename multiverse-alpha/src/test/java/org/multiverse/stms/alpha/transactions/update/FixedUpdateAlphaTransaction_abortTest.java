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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasListeners;

public class FixedUpdateAlphaTransaction_abortTest {

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
        FixedUpdateAlphaTransaction.Config config = new FixedUpdateAlphaTransaction.Config(
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
        return new FixedUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = startSutTransaction(10);
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenReadsAreDone() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenWritesAreDone() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        tx.openForWrite(ref1);
        tx.openForWrite(ref2);
        tx.abort();

        assertIsAborted(tx);
    }

    @Test
    public void whenPendingWrite_thenWriteDiscarded() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction(10);
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
                .setAutomaticReadTracking(true).build().start();
        listenTx.openForRead(ref);
        listenTx.registerRetryLatch(latch);

        AlphaTransaction tx = startSutTransaction(10);
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;
        tx.abort();

        assertHasListeners(ref, latch);
    }

    @Test
    public void whenPreparedWithLockedResources_thenResourcesFreed() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction(10);
        ref.inc(tx);
        tx.prepare();

        tx.abort();
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
    }

}

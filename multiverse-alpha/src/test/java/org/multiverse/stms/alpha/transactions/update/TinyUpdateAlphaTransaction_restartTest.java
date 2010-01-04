package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

public class TinyUpdateAlphaTransaction_restartTest {

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
                stmConfig.commitLockPolicy, true, optimalSize,true,true,true,true);
        return new TinyUpdateAlphaTransaction(config);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = startSutTransaction();
        tx.restart();

        assertRestartedCorrectly(tx);
    }

    @Test
    public void whenOpenForRead_thenAttachedIsCleared() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);
        tx.restart();

        assertRestartedCorrectly(tx);
    }

    @Test
    public void whenOpenForWrite_thenAttachedIsCleared() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);
        tx.restart();

        assertRestartedCorrectly(tx);
    }

    @Test
    public void whenOtherTxCommitted_thenTxReadVersionIncreased() {
        AlphaTransaction tx = startSutTransaction();

        stmConfig.clock.tick();

        tx.restart();
        assertRestartedCorrectly(tx);
    }

    @Test
    public void whenPendingWrites_theseAreDiscarded() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal dirty = (ManualRefTranlocal) tx.openForWrite(ref);
        dirty.value++;
        tx.restart();

        assertRestartedCorrectly(tx);
        assertSame(committed, ref.___load());
    }

    private void assertRestartedCorrectly(AlphaTransaction tx) {
        assertIsActive(tx);
        assertEquals(stm.getVersion(), tx.getReadVersion());
        assertNull(getField(tx, "attached"));
    }
}

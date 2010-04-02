package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

public class MonoUpdateAlphaTransaction_restartTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);

    }

    public MonoUpdateAlphaTransaction startSutTransaction() {
        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        UpdateAlphaTransactionConfiguration config = new UpdateAlphaTransactionConfiguration(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                speculativeConfig,
                stmConfig.maxRetryCount, true, true, true, true, true, true);
        return new MonoUpdateAlphaTransaction(config);
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

    @Test
    public void whenPreparedWithLockedResources_thenResourcesFreed() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.inc(tx);
        tx.prepare();

        tx.restart();
        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
    }
}

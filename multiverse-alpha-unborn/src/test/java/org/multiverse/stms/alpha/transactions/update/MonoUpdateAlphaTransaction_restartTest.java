package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsNew;
import static org.multiverse.TestUtils.getField;

public class MonoUpdateAlphaTransaction_restartTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MonoUpdateAlphaTransaction createSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new MonoUpdateAlphaTransaction(config);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = createSutTransaction();
        tx.reset();

        assertRestartedCorrectly(tx);
    }

    @Test
    public void whenOpenForRead_thenAttachedIsCleared() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref);
        tx.reset();

        assertRestartedCorrectly(tx);
    }

    @Test
    public void whenOpenForWrite_thenAttachedIsCleared() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForWrite(ref);
        tx.reset();

        assertRestartedCorrectly(tx);
    }

    @Test
    public void whenOtherTxCommitted_thenTxReadVersionIncreased() {
        AlphaTransaction tx = createSutTransaction();

        stmConfig.clock.tick();

        tx.reset();
        assertRestartedCorrectly(tx);
    }

    @Test
    public void whenPendingWrites_theseAreDiscarded() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        AlphaTransaction tx = createSutTransaction();
        ManualRefTranlocal dirty = (ManualRefTranlocal) tx.openForWrite(ref);
        dirty.value++;
        tx.reset();

        assertRestartedCorrectly(tx);
        assertSame(committed, ref.___load());
    }

    private void assertRestartedCorrectly(AlphaTransaction tx) {
        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
        assertNull(getField(tx, "attached"));
    }

    @Test
    public void whenPreparedWithLockedResources_thenResourcesFreed() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        ref.inc(tx);
        tx.prepare();

        tx.reset();
        assertIsNew(tx);
        assertNull(ref.___getLockOwner());
    }
}

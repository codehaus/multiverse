package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.util.Map;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

public class MapReadonlyAlphaTransaction_restartTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapReadonlyAlphaTransaction startSutTransaction() {
        MapReadonlyAlphaTransaction.Config config = new MapReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount, true);
        return new MapReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenUnused() {
        long version = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();
        tx.restart();

        assertIsActive(tx);
        assertSame(version, tx.getReadVersion());
        assertEquals(version, stm.getVersion());
        Map readMap = (Map) getField(tx, "attachedMap");
        assertTrue(readMap.isEmpty());
    }

    @Test
    public void whenUsed_thenReadMapIsCleared() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.restart();

        Map readMap = (Map) getField(tx, "attachedMap");
        assertTrue(readMap.isEmpty());
    }

    @Test
    public void whenOtherTxCommittedAfterThisTxStarted_thenReadVersionIncreased() {
        AlphaTransaction tx = startSutTransaction();

        stmConfig.clock.tick();
        tx.restart();

        assertEquals(stm.getVersion(), tx.getReadVersion());
    }

    @Test
    public void whenAborted_thenTxActivated() {
        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        tx.restart();
        assertIsActive(tx);
    }

    @Test
    public void whenCommitted_thenTxActivated() {
        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        tx.restart();
        assertIsActive(tx);
    }
}

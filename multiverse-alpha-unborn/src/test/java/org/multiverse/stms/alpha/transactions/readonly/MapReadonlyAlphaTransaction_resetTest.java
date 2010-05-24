package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.assertIsNew;
import static org.multiverse.TestUtils.getField;

public class MapReadonlyAlphaTransaction_resetTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapReadonlyAlphaTransaction createSutTransaction() {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true);
        return new MapReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenUnused() {
        long version = stm.getVersion();
        AlphaTransaction tx = createSutTransaction();
        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
        assertEquals(version, stm.getVersion());
        Map readMap = (Map) getField(tx, "attachedMap");
        assertTrue(readMap.isEmpty());
    }

    @Test
    public void whenUsed_thenReadMapIsCleared() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.reset();

        Map readMap = (Map) getField(tx, "attachedMap");
        assertTrue(readMap.isEmpty());
    }

    @Test
    public void whenAborted_thenTxActivated() {
        AlphaTransaction tx = createSutTransaction();
        tx.abort();

        tx.reset();
        assertIsNew(tx);
    }

    @Test
    public void whenCommitted_thenTxActivated() {
        AlphaTransaction tx = createSutTransaction();
        tx.commit();

        tx.reset();
        assertIsNew(tx);
    }
}

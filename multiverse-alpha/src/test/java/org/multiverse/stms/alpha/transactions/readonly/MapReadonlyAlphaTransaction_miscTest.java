package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.assertIsActive;

public class MapReadonlyAlphaTransaction_miscTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapReadonlyAlphaTransaction startTransactionUnderTest() {
        MapReadonlyAlphaTransaction.Config config = new MapReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.maxRetryCount, true);
        return new MapReadonlyAlphaTransaction(config);
    }

    @Test
    public void testConstruction() {
        AlphaTransaction tx = startTransactionUnderTest();
        assertIsActive(tx);
        assertEquals(stm.getVersion(), tx.getReadVersion());
        assertTrue(tx.getConfig().isReadonly());
        assertTrue(tx.getConfig().automaticReadTracking());

    }
}

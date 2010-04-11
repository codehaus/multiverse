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
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true);
        return new MapReadonlyAlphaTransaction(config);
    }

    @Test
    public void testConstruction() {
        AlphaTransaction tx = startTransactionUnderTest();
        assertIsActive(tx);
        assertEquals(stm.getVersion(), tx.getReadVersion());
        assertTrue(tx.getConfiguration().isReadonly());
        assertTrue(tx.getConfiguration().isAutomaticReadTrackingEnabled());

    }
}

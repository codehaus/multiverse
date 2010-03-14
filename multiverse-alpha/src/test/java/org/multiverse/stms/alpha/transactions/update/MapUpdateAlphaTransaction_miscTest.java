package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsActive;

public class MapUpdateAlphaTransaction_miscTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapUpdateAlphaTransaction startSutTransaction() {
        MapUpdateAlphaTransaction.Config config = new MapUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                false, true, true, true, true);
        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    public void testConstruction() {
        long expectedReadVersion = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        assertEquals(expectedReadVersion, tx.getReadVersion());
        assertIsActive(tx);
    }
}

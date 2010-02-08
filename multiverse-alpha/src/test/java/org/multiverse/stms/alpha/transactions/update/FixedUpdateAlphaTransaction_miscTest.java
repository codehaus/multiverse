package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsActive;

public class FixedUpdateAlphaTransaction_miscTest {
    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public FixedUpdateAlphaTransaction startSutTransaction() {
        OptimalSize optimalSize = new OptimalSize(10);

        FixedUpdateAlphaTransaction.Config config = new FixedUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                false, optimalSize, true, true, true, true, 50);
        return new FixedUpdateAlphaTransaction(config, 10);
    }

    @Test
    public void testConstruction() {
        long expectedReadVersion = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        assertEquals(expectedReadVersion, tx.getReadVersion());
        assertIsActive(tx);
    }
}

package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsActive;

public class NonTrackingReadonlyAlphaTransaction_miscTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public NonTrackingReadonlyAlphaTransaction startSutTransaction() {
        NonTrackingReadonlyAlphaTransaction.Config config = new NonTrackingReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.maxRetryCount);
        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    @Test
    public void start() {
        long version = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();
        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }
}

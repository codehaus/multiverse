package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsAborted;

/**
 * @author Peter Veentjer
 */
public class NonTrackingReadonlyAlphaTransaction_abortTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public NonTrackingReadonlyAlphaTransaction startSutTransaction() {
        ReadonlyAlphaTransactionConfig config = new ReadonlyAlphaTransactionConfig(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                new OptimalSize(10, 100),
                stmConfig.maxRetryCount, false, false);

        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenUnused() {
        long startVersion = stm.getVersion();
        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        assertEquals(startVersion, stm.getVersion());
        assertIsAborted(tx);
    }

    @Test
    public void whenUsed() {
        ManualRef ref = new ManualRef(stm);
        long startVersion = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);
        tx.abort();

        assertEquals(startVersion, stm.getVersion());
        assertIsAborted(tx);
    }
}

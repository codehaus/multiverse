package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.testIncomplete;

/**
 * @author Peter Veentjer
 */
public class NonTrackingReadonlyAlphaTransaction_restartTest {

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
                stmConfig.profiler,
                stmConfig.maxRetryCount);
        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenUnusedAndNoOtherUpdates() {
        AlphaTransaction tx = startSutTransaction();

        long expectedReadVersion = stm.getVersion();
        tx.restart();
        assertEquals(expectedReadVersion, tx.getReadVersion());
        assertEquals(expectedReadVersion, stm.getVersion());
        assertIsActive(tx);
    }

    @Test
    public void whenUnusedAndOtherUpdates() {
        AlphaTransaction tx = startSutTransaction();

        stmConfig.clock.tick();

        long expectedReadVersion = stm.getVersion();
        tx.restart();
        assertEquals(expectedReadVersion, tx.getReadVersion());
        assertEquals(expectedReadVersion, stm.getVersion());
        assertIsActive(tx);
    }


    @Test
    public void whenUsedAndNoOtherUpdates() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);

        testIncomplete();
    }

    @Test
    public void whenAborted() {
        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        long version = stm.getVersion();
        tx.restart();

        //todo: read version
        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
    }

    @Test
    public void whenCommitted() {
        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        long version = stm.getVersion();
        tx.restart();

        //todo: read version
        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
    }
}

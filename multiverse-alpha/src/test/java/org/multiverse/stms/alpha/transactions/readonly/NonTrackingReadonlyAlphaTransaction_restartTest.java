package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsActive;

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
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock)
                .withAutomaticReadTracking(false);

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
    public void whenPrepared() {
        AlphaTransaction tx = startSutTransaction();
        tx.prepare();

        stmConfig.clock.tick();

        long version = stm.getVersion();
        tx.restart();

        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
        assertEquals(version, tx.getReadVersion());
    }

    @Test
    public void whenAborted() {
        AlphaTransaction tx = startSutTransaction();
        tx.abort();
        stmConfig.clock.tick();

        long version = stm.getVersion();
        tx.restart();

        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
        assertEquals(version, tx.getReadVersion());
    }

    @Test
    public void whenCommitted() {
        AlphaTransaction tx = startSutTransaction();
        tx.commit();
        stmConfig.clock.tick();

        long version = stm.getVersion();
        tx.restart();

        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
        assertEquals(version, tx.getReadVersion());
    }
}

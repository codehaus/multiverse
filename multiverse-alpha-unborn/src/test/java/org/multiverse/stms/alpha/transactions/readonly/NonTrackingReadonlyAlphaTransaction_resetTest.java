package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsNew;

/**
 * @author Peter Veentjer
 */
public class NonTrackingReadonlyAlphaTransaction_resetTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public NonTrackingReadonlyAlphaTransaction createSutTransaction() {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, false);

        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenUnusedAndNoOtherUpdates() {
        AlphaTransaction tx = createSutTransaction();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenUnusedAndOtherUpdates() {
        AlphaTransaction tx = createSutTransaction();

        stmConfig.clock.tick();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenPrepared() {
        AlphaTransaction tx = createSutTransaction();
        tx.prepare();

        stmConfig.clock.tick();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenAborted() {
        AlphaTransaction tx = createSutTransaction();
        tx.abort();
        stmConfig.clock.tick();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenCommitted() {
        AlphaTransaction tx = createSutTransaction();
        tx.commit();
        stmConfig.clock.tick();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }
}

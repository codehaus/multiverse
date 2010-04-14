package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsActive;

public class ArrayUpdateAlphaTransaction_miscTest {
    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public ArrayUpdateAlphaTransaction startSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new ArrayUpdateAlphaTransaction(config, 10);
    }

    @Test
    public void testConstruction() {
        long expectedReadVersion = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        assertEquals(expectedReadVersion, tx.getReadVersion());
        assertIsActive(tx);
    }
}

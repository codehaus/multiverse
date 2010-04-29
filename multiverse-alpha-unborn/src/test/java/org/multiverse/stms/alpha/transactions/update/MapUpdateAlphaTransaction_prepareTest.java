package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsCommitted;

public class MapUpdateAlphaTransaction_prepareTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MapUpdateAlphaTransaction startSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    public void whenDirty_thenResourcesLocked() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.inc(tx);
        tx.prepare();

        assertSame(tx, ref.___getLockOwner());
    }

    @Test
    public void whenNonDirty_thenNoResourcesLocked() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);
        tx.prepare();

        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenPrepared_thenCommitSuccess() {
        ManualRef ref = new ManualRef(stm, 1);

        AlphaTransaction tx = startSutTransaction();
        ref.inc(tx);
        tx.prepare();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(2, ref.get(stm));
    }
}

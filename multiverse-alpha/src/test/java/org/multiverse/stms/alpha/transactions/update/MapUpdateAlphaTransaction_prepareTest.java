package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsCommitted;

public class MapUpdateAlphaTransaction_prepareTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapUpdateAlphaTransaction startSutTransaction() {
        OptimalSize optimalSize = new OptimalSize(1, 100);
        UpdateAlphaTransactionConfig config = new UpdateAlphaTransactionConfig(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                stmConfig.commitLockPolicy,
                null,
                optimalSize,
                stmConfig.maxRetryCount, true, true, true, true, true);

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

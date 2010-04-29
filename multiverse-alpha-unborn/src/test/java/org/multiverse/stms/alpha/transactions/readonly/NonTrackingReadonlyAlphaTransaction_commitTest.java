package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertIsCommitted;

/**
 * @author Peter Veentjer
 */
public class NonTrackingReadonlyAlphaTransaction_commitTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public NonTrackingReadonlyAlphaTransaction startSutTransaction() {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, false);

        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    @Test
    public void commitDoesNotLockTransactionalObjects() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);
        ref.resetLockInfo();
        tx.commit();

        ref.assertNoLockAcquired();
        ref.assertNoLocksReleased();
    }

    @Test
    public void whenUnused() {
        long startVersion = stm.getVersion();
        Transaction tx = startSutTransaction();
        tx.commit();

        assertEquals(startVersion, stm.getVersion());
        assertIsCommitted(tx);
    }

    @Test
    public void whenUsed() {
        ManualRef ref = new ManualRef(stm);
        ManualRefTranlocal committed = (ManualRefTranlocal) ref.___load();

        long startVersion = stm.getVersion();

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);
        tx.commit();

        assertSame(committed, ref.___load());
        assertEquals(startVersion, stm.getVersion());
        assertIsCommitted(tx);
    }

    @Test
    public void whenConflictingChangesAreMade_thenCommitSuccess() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);

        //conflicting update
        ref.inc(stm);
        long startVersion = stm.getVersion();

        tx.commit();
        assertIsCommitted(tx);
        assertEquals(startVersion, stm.getVersion());
    }
}

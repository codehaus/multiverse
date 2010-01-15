package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsCommitted;

public class FixedReadonlyAlphaTransaction_commitTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public FixedReadonlyAlphaTransaction startTransactionUnderTest(int size) {
        optimalSize.set(size);

        FixedReadonlyAlphaTransaction.Config config = new FixedReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount, true, optimalSize, size);

        return new FixedReadonlyAlphaTransaction(config, size);
    }

    @Test
    public void whenEmpty() {
        AlphaTransaction tx = startTransactionUnderTest(10);

        long version = stm.getVersion();

        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNoConflictingReads_thenCommitSuccess() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.openForRead(ref);

        long version = stm.getVersion();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenConflictingWritesAreFoundAfterOpenForRead_thenCommitSuccess() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.openForRead(ref);

        //conflicting write
        ref.inc(stm);

        long version = stm.getVersion();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }
}

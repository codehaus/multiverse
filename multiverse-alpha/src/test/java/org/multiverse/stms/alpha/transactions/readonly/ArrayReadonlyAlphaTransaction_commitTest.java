package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsCommitted;

public class ArrayReadonlyAlphaTransaction_commitTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private SpeculativeConfiguration speculativeConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        speculativeConfig = new SpeculativeConfiguration(1);
    }

    public ArrayReadonlyAlphaTransaction startTransactionUnderTest(int maximumSize) {
        ReadonlyAlphaTransactionConfiguration config = new ReadonlyAlphaTransactionConfiguration(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                new SpeculativeConfiguration(maximumSize),
                stmConfig.maxRetryCount, false, true);
        return new ArrayReadonlyAlphaTransaction(config, maximumSize);
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

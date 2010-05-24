package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsCommitted;

public class ArrayReadonlyAlphaTransaction_commitTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public ArrayReadonlyAlphaTransaction createSutTransaction(int maximumSize) {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true);
        return new ArrayReadonlyAlphaTransaction(config, maximumSize);
    }

    @Test
    public void whenEmpty() {
        AlphaTransaction tx = createSutTransaction(10);

        long version = stm.getVersion();

        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNoConflictingReads_thenCommitSuccess() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction(10);
        tx.openForRead(ref);

        long version = stm.getVersion();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenConflictingWritesAreFoundAfterOpenForRead_thenCommitSuccess() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction(10);
        tx.openForRead(ref);

        //conflicting write
        ref.inc(stm);

        long version = stm.getVersion();
        tx.commit();

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }
}

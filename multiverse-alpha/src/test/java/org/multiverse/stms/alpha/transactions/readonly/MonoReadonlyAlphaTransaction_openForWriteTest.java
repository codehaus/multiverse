package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

public class MonoReadonlyAlphaTransaction_openForWriteTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createFastConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public MonoReadonlyAlphaTransaction startSutTransaction() {
        MonoReadonlyAlphaTransaction.Config config = new MonoReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.maxRetryCount, true, optimalSize);
        return new MonoReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenUnused_thenReadonlyException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsActive(tx);
        assertNull(getField(tx, "attached"));
    }

    @Test(expected = DeadTransactionException.class)
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        tx.openForWrite(ref);
    }

    @Test(expected = DeadTransactionException.class)
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        tx.openForWrite(ref);
    }
}

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

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;

public class ArrayReadonlyAlphaTransaction_openForWriteTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public ArrayReadonlyAlphaTransaction startTransactionUnderTest(int size) {
        optimalSize.set(size);

        ArrayReadonlyAlphaTransaction.Config config = new ArrayReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.maxRetryCount, true, optimalSize, size);

        return new ArrayReadonlyAlphaTransaction(config, size);
    }

    @Test
    public void whenActiveAndNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = startTransactionUnderTest(10);

        try {
            tx.openForWrite(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void withActive_thenReadonlyException() {
        ManualRef ref = new ManualRef(stm, 0);

        AlphaTransaction tx = startTransactionUnderTest(10);

        try {
            tx.openForWrite(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.commit();

        try {
            tx.openForRead(value);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }


    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.abort();

        try {
            tx.openForRead(value);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}

package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

public class FixedReadonlyAlphaTransaction_resetTest {

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
                stmConfig.restartBackoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount, true, optimalSize, size);

        return new FixedReadonlyAlphaTransaction(config, size);
    }

    @Test
    public void whenUnused(){
        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.restart();

        assertEquals(0, getField(tx,"firstFreeIndex"));
        assertIsActive(tx);
    }

    @Test
    public void whenOtherTxCommitted_thenReadVersionUpdated(){
        AlphaTransaction tx = startTransactionUnderTest(10);

        stmConfig.clock.tick();

        tx.restart();
        assertEquals(stm.getVersion(), tx.getReadVersion());
    }
    
    @Test
    public void whenUsed(){
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);

        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.openForRead(ref3);
        tx.restart();

        assertEquals(0, getField(tx, "firstFreeIndex"));
        AlphaTranlocal[] attached = (AlphaTranlocal[]) getField(tx,"attachedArray");
        for(int k=0;k<attached.length;k++){
            assertNull(attached[k]);
        }
    }
}

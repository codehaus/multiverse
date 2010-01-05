package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

public class FixedUpdateAlphaTransaction_restartTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public AlphaTransaction startSutTransaction(int size) {
        optimalSize.set(size);
        FixedUpdateAlphaTransaction.Config config = new FixedUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.restartBackoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                true,
                optimalSize,
                true,true,true,true,size
        );
        return new FixedUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = startSutTransaction(10);
        tx.restart();

        assertIsActive(tx);
        assertEquals(0, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenUsed() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRefTranlocal committed1 = (ManualRefTranlocal) ref1.___load();
        ManualRef ref2 = new ManualRef(stm);
        ManualRefTranlocal committed2 = (ManualRefTranlocal) ref2.___load();

        AlphaTransaction tx = startSutTransaction(10);
        tx.openForWrite(ref1);
        tx.openForRead(ref2);

        tx.restart();
        assertIsActive(tx);
        assertEquals(0, getField(tx,"firstFreeIndex"));
        assertSame(committed1, ref1.___load());
        assertSame(committed2, ref2.___load());
    }
}

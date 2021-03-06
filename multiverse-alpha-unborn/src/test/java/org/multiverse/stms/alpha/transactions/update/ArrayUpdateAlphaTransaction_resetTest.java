package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public class ArrayUpdateAlphaTransaction_resetTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public AlphaTransaction createSutTransaction(int size) {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new ArrayUpdateAlphaTransaction(config, size);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = createSutTransaction(10);
        tx.reset();

        assertIsNew(tx);
        assertEquals(0, getField(tx, "firstFreeIndex"));
    }

    @Test
    public void whenUsed() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRefTranlocal committed1 = (ManualRefTranlocal) ref1.___load();
        ManualRef ref2 = new ManualRef(stm);
        ManualRefTranlocal committed2 = (ManualRefTranlocal) ref2.___load();

        AlphaTransaction tx = createSutTransaction(10);
        tx.openForWrite(ref1);
        tx.openForRead(ref2);

        tx.reset();
        assertIsNew(tx);
        assertEquals(0, getField(tx, "firstFreeIndex"));
        assertSame(committed1, ref1.___load());
        assertSame(committed2, ref2.___load());
    }

    @Test
    public void whenPreparedWithLockedResources_thenResourcesFreed() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction(10);
        ref.inc(tx);
        tx.prepare();

        tx.abort();
        assertIsAborted(tx);
        assertNull(ref.___getLockOwner());
    }

}

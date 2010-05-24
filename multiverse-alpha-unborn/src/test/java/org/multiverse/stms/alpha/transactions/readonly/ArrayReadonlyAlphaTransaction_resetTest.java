package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.AlphaTranlocal;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.TestUtils.assertIsNew;
import static org.multiverse.TestUtils.getField;

public class ArrayReadonlyAlphaTransaction_resetTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public ArrayReadonlyAlphaTransaction createSutTransaction() {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true);
        return new ArrayReadonlyAlphaTransaction(config, 100);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = createSutTransaction();
        tx.reset();

        assertEquals(0, getField(tx, "firstFreeIndex"));
        assertIsNew(tx);
    }

    @Test
    public void whenOtherTxCommitted_thenReadVersionUpdated() {
        AlphaTransaction tx = createSutTransaction();

        stmConfig.clock.tick();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenUsed() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.openForRead(ref3);
        tx.reset();

        assertEquals(0, getField(tx, "firstFreeIndex"));
        AlphaTranlocal[] attached = (AlphaTranlocal[]) getField(tx, "attachedArray");
        for (int k = 0; k < attached.length; k++) {
            assertNull(attached[k]);
        }
    }
}

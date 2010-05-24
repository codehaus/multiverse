package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.TestUtils.assertIsNew;
import static org.multiverse.TestUtils.getField;

/**
 * @author Peter Veentjer
 */
public class MapUpdateAlphaTransaction_resetTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MapUpdateAlphaTransaction createSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = createSutTransaction();
        tx.start();
        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenUsed() {
        ManualRef ref = new ManualRef(stm, 1);

        AlphaTransaction tx = createSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;

        tx.reset();
        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenAborted() {
        AlphaTransaction tx = createSutTransaction();
        tx.abort();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenPreparedWithLockedResources_thenResourcesFreed() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        ref.inc(tx);
        tx.prepare();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenCommitted() {
        AlphaTransaction tx = createSutTransaction();
        tx.commit();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenVersionUpdatedByOtherTx_thenTxUpdatesReadVersion() {
        AlphaTransaction tx = createSutTransaction();

        stmConfig.clock.tick();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenReadWriteMapNonEmpty_thenItIsCleared() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref1);
        tx.openForWrite(ref2);
        tx.reset();

        Map readWriteMap = (Map) getField(tx, "attachedMap");
        assertTrue(readWriteMap.isEmpty());
    }
}

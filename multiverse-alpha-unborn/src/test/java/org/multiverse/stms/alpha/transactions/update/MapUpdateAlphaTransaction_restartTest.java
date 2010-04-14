package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRefTranlocal;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import java.util.Map;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.getField;

/**
 * @author Peter Veentjer
 */
public class MapUpdateAlphaTransaction_restartTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapUpdateAlphaTransaction startSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new MapUpdateAlphaTransaction(config);
    }

    @Test
    public void whenUnused() {
        AlphaTransaction tx = startSutTransaction();
        tx.restart();

        assertEquals(stm.getVersion(), tx.getReadVersion());
        assertIsActive(tx);
    }

    @Test
    public void whenUsed() {
        ManualRef ref = new ManualRef(stm, 1);

        AlphaTransaction tx = startSutTransaction();
        ManualRefTranlocal tranlocal = (ManualRefTranlocal) tx.openForWrite(ref);
        tranlocal.value++;

        tx.restart();
        assertIsActive(tx);
        assertEquals(stm.getVersion(), tx.getReadVersion());
    }

    @Test
    public void whenAborted() {
        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        tx.restart();
        assertIsActive(tx);
        assertEquals(stm.getVersion(), tx.getReadVersion());
    }

    @Test
    public void whenPreparedWithLockedResources_thenResourcesFreed() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        ref.inc(tx);
        tx.prepare();

        tx.restart();
        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenCommitted() {
        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        tx.restart();
        assertIsActive(tx);
        assertEquals(stm.getVersion(), tx.getReadVersion());
    }

    @Test
    public void whenVersionUpdatedByOtherTx_thenTxUpdatesReadVersion() {
        AlphaTransaction tx = startSutTransaction();

        stmConfig.clock.tick();

        tx.restart();

        assertIsActive(tx);
        assertEquals(stm.getVersion(), tx.getReadVersion());
    }

    @Test
    public void whenReadWriteMapNonEmpty_thenItIsCleared() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref1);
        tx.openForWrite(ref2);
        tx.restart();

        Map readWriteMap = (Map) getField(tx, "attachedMap");
        assertTrue(readWriteMap.isEmpty());
    }
}

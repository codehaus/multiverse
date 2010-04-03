package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class MapReadonlyAlphaTransaction_openForConstructionTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapReadonlyAlphaTransaction startTransactionUnderTest() {
        return startTransactionUnderTest(new SpeculativeConfiguration(100));
    }

    public MapReadonlyAlphaTransaction startTransactionUnderTest(SpeculativeConfiguration speculativeConfig) {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock)
                .withSpeculativeConfig(speculativeConfig);
        return new MapReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenActiveAndNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = startTransactionUnderTest();

        try {
            tx.openForConstruction(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void withExplicitReadlonly_thenReadonlyException() {
        ManualRef ref = new ManualRef(stm, 0);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, true, true, 100);
        AlphaTransaction tx = startTransactionUnderTest(speculativeConfig);

        long version = stm.getVersion();
        try {
            tx.openForConstruction(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void withSpeculativeReadlonly_thenSpeculativeConfigurationFailure() {
        ManualRef ref = new ManualRef(stm, 0);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(true, false, false, 100);
        AlphaTransaction tx = startTransactionUnderTest(speculativeConfig);

        long version = stm.getVersion();
        try {
            tx.openForConstruction(ref);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertFalse(speculativeConfig.isReadonly());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.prepare();

        long version = stm.getVersion();
        try {
            tx.openForConstruction(value);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.commit();

        long version = stm.getVersion();
        try {
            tx.openForConstruction(value);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startTransactionUnderTest();
        tx.abort();

        long version = stm.getVersion();
        try {
            tx.openForConstruction(value);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
    }
}

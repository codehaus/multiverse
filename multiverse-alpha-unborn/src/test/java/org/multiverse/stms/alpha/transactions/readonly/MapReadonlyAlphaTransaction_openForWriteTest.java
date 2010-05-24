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
public class MapReadonlyAlphaTransaction_openForWriteTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MapReadonlyAlphaTransaction createSutTransaction() {
        return createSutTransaction(new SpeculativeConfiguration(100));
    }

    public MapReadonlyAlphaTransaction createSutTransaction(SpeculativeConfiguration speculativeConfig) {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true)
                .withSpeculativeConfig(speculativeConfig);
        return new MapReadonlyAlphaTransaction(config);
    }

    @Test
    public void withNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = createSutTransaction();
        tx.start();

        long expectedVersion = stm.getVersion();

        try {
            tx.openForWrite(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenExplicitReadonly_thenReadonlyException() {
        ManualRef ref = new ManualRef(stm, 0);
        long expectedVersion = stm.getVersion();

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, true, true, 100);
        AlphaTransaction tx = createSutTransaction(speculativeConfig);
        try {
            tx.openForWrite(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsActive(tx);
        assertEquals(expectedVersion, stm.getVersion());
        assertTrue(speculativeConfig.isReadonly());
    }

    @Test
    public void whenSpeculativeReadonly_thenReadonlyException() {
        ManualRef ref = new ManualRef(stm, 0);
        long expectedVersion = stm.getVersion();

        SpeculativeConfiguration speculativeConfiguration = new SpeculativeConfiguration(100);
        AlphaTransaction tx = createSutTransaction(speculativeConfiguration);
        try {
            tx.openForWrite(ref);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertIsActive(tx);
        assertEquals(expectedVersion, stm.getVersion());
        assertFalse(speculativeConfiguration.isReadonly());
    }

    @Test
    public void whenPrepared() {
        ManualRef ref = new ManualRef(stm, 10);

        AlphaTransaction tx = createSutTransaction();
        tx.prepare();

        long expectedVersion = stm.getVersion();
        try {
            tx.openForWrite(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 10);

        AlphaTransaction tx = createSutTransaction();
        tx.commit();

        long expectedVersion = stm.getVersion();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef ref = new ManualRef(stm, 10);
        long expectedVersion = stm.getVersion();

        AlphaTransaction tx = createSutTransaction();
        tx.abort();

        try {
            tx.openForWrite(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(expectedVersion, stm.getVersion());
    }
}

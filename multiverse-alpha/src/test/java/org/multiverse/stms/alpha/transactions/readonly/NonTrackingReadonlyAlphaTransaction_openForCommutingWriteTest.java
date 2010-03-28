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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class NonTrackingReadonlyAlphaTransaction_openForCommutingWriteTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public NonTrackingReadonlyAlphaTransaction startSutTransaction() {
        return startSutTransaction(new SpeculativeConfiguration(100));
    }

    public NonTrackingReadonlyAlphaTransaction startSutTransaction(SpeculativeConfiguration speculativeConfiguration) {
        ReadonlyAlphaTransactionConfiguration config = new ReadonlyAlphaTransactionConfiguration(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                speculativeConfiguration,
                stmConfig.maxRetryCount, false, false);
        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenActiveAndNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = startSutTransaction();

        try {
            tx.openForCommutingWrite(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void withExplicitReadonly_thenReadonlyException() {
        ManualRef ref = new ManualRef(stm, 0);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, true, true, 100);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);

        long version = stm.getVersion();
        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertTrue(speculativeConfig.isReadonly());
    }

    @Test
    public void whenSpeculativeReadonly_thenSpeculativeConfigurationFailure() {
        ManualRef value = new ManualRef(stm, 10);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(100);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);

        long version = stm.getVersion();
        try {
            tx.openForCommutingWrite(value);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertEquals(version, stm.getVersion());
        assertIsActive(tx);
        assertFalse(speculativeConfig.isReadonly());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startSutTransaction();
        tx.prepare();

        long version = stm.getVersion();
        try {
            tx.openForCommutingWrite(value);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertIsPrepared(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startSutTransaction();
        tx.commit();

        long version = stm.getVersion();
        try {
            tx.openForCommutingWrite(value);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = startSutTransaction();
        tx.abort();

        long version = stm.getVersion();
        try {
            tx.openForCommutingWrite(value);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertEquals(version, stm.getVersion());
    }
}

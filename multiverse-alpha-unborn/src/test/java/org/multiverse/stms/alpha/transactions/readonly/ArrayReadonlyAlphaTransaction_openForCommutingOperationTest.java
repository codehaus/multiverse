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
public class ArrayReadonlyAlphaTransaction_openForCommutingOperationTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public ArrayReadonlyAlphaTransaction createSutTransaction(int size) {
        SpeculativeConfiguration speculativeConfiguration = new SpeculativeConfiguration(100);
        speculativeConfiguration.setOptimalSize(size);
        return createSutTransaction(speculativeConfiguration);
    }

    public ArrayReadonlyAlphaTransaction createSutTransaction(SpeculativeConfiguration speculativeConfiguration) {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true)
                .withSpeculativeConfig(speculativeConfiguration);

        return new ArrayReadonlyAlphaTransaction(config, speculativeConfiguration.getOptimalSize());
    }

    @Test
    public void whenNewAndNullTxObject_thenNullPointerExceptionAndRemainNew() {
        AlphaTransaction tx = createSutTransaction(10);

        try {
            tx.openForCommutingWrite(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsNew(tx);
    }

    @Test
    public void whenNew_thenNotStarted() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction(10);

        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {

        }

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenActiveAndNullTxObject_thenNullPointerException() {
        AlphaTransaction tx = createSutTransaction(10);
        tx.start();

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
        AlphaTransaction tx = createSutTransaction(speculativeConfig);
        tx.start();

        long version = stm.getVersion();
        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void withSpeculativeReadonly_thenSpeculativeConfigurationFailure() {
        ManualRef ref = new ManualRef(stm, 0);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(true, false, false, 100);
        AlphaTransaction tx = createSutTransaction(speculativeConfig);
        tx.start();

        long version = stm.getVersion();
        try {
            tx.openForCommutingWrite(ref);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertIsActive(tx);
        assertEquals(version, stm.getVersion());
        assertFalse(speculativeConfig.isReadonly());
    }

    @Test
    public void whenPrepared() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = createSutTransaction(10);
        tx.prepare();

        long version = stm.getVersion();
        try {
            tx.openForCommutingWrite(value);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsPrepared(tx);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ManualRef value = new ManualRef(stm, 10);

        AlphaTransaction tx = createSutTransaction(10);
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

        AlphaTransaction tx = createSutTransaction(10);
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

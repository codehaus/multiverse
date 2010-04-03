package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;

/**
 * @author Peter Veentjer
 */
public class NonTrackingReadonlyAlphaTransaction_registerRetryLatchTest {

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
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock)
                .withAutomaticReadTracking(false)
                .withSpeculativeConfig(speculativeConfiguration);

        return new NonTrackingReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenSpeculativeNonAutomaticReadTrackingAndUsed_thenSpeculativeConfigurationFailure() {
        ManualRef ref = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, true, false, 100);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);
        tx.openForRead(ref);

        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertFalse(latch.isOpen());
        assertIsActive(tx);
        assertNull(ref.___getListeners());
        assertTrue(speculativeConfig.isAutomaticReadTracking());
    }

    @Test
    public void whenSpeculativeNonAutomaticReadTrackingAndUnused_thenSpeculativeConfigurationFailure() {
        ManualRef ref = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, true, false, 100);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);

        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertFalse(latch.isOpen());
        assertIsActive(tx);
        assertNull(ref.___getListeners());
        assertTrue(speculativeConfig.isAutomaticReadTracking());
    }

    @Test
    public void whenExplicitNonAutomaticReadTrackingAndUnused_thenNoRetryPossibleException() {
        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, false, false, 100);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);

        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertFalse(latch.isOpen());
        assertIsActive(tx);
        assertFalse(speculativeConfig.isAutomaticReadTracking());
    }

    @Test
    public void whenExplicitNonAutomaticReadTrackingAndUsed_thenNoRetryPossibleException() {
        ManualRef ref = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(true, false, true, 100);
        AlphaTransaction tx = startSutTransaction(speculativeConfig);
        tx.openForRead(ref);

        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertFalse(latch.isOpen());
        assertIsActive(tx);
        assertNull(ref.___getListeners());
        assertFalse(speculativeConfig.isAutomaticReadTracking());
    }
}

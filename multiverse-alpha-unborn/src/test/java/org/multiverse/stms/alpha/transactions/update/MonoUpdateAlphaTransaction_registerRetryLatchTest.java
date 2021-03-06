package org.multiverse.stms.alpha.transactions.update;

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
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasListeners;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasNoListeners;

public class MonoUpdateAlphaTransaction_registerRetryLatchTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stmConfig.maxRetries = 10;
        stm = new AlphaStm(stmConfig);
    }

    public MonoUpdateAlphaTransaction createSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new MonoUpdateAlphaTransaction(config);
    }

    public MonoUpdateAlphaTransaction createSutTransactionWithoutAutomaticReadTracking(SpeculativeConfiguration speculativeConfig) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withExplictRetryAllowed(true)
                .withReadTrackingEnabled(false)
                .withSpeculativeConfiguration(speculativeConfig);

        return new MonoUpdateAlphaTransaction(config);
    }

    @Test
    public void whenNullLatch_thenNullPointerException() {
        AlphaTransaction tx = createSutTransaction();

        try {
            tx.registerRetryLatch(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenExplicitNoAutomaticReadtracking_thenNoRetryPossibleException() {
        ManualRef ref = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, false, false, 100);
        AlphaTransaction tx = createSutTransactionWithoutAutomaticReadTracking(speculativeConfig);
        tx.openForWrite(ref);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertFalse(latch.isOpen());
        assertFalse(speculativeConfig.isReadTrackingEnabled());
    }

    @Test
    public void whenSpeculativeNoAutomaticReadtracking_thenSpeculativeConfigurationFailure() {
        ManualRef ref = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, true, false, 100);
        AlphaTransaction tx = createSutTransactionWithoutAutomaticReadTracking(speculativeConfig);
        tx.openForWrite(ref);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertFalse(latch.isOpen());
        assertTrue(speculativeConfig.isReadTrackingEnabled());
    }

    @Test
    public void whenUnused_thenNoRetryPossibleException() {
        AlphaTransaction tx = createSutTransaction();
        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenListenerAdded() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref);

        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenListenerAdded() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForWrite(ref);

        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
    }

    @Test
    public void whenExplicitRetryNotAllowed() {
        ManualRef ref = new ManualRef(stm);

        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withExplictRetryAllowed(false);

        AlphaTransaction tx = new MonoUpdateAlphaTransaction(config);
        tx.openForRead(ref);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsActive(tx);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenFresh_thenNoRetryPossibleException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = createSutTransaction();
        tx.openForConstruction(ref);
        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsActive(tx);
        assertFalse(latch.isOpen());
        assertHasNoListeners(ref);
    }

    @Test
    public void whenListenersAlreadyPresent_newListenerAppended() {
        ManualRef ref = new ManualRef(stm);
        Latch latch1 = new CheapLatch();
        Latch latch2 = new CheapLatch();

        AlphaTransaction tx1 = createSutTransaction();
        tx1.openForRead(ref);
        tx1.registerRetryLatch(latch1);

        AlphaTransaction tx2 = createSutTransaction();
        tx2.openForRead(ref);
        tx2.registerRetryLatch(latch2);

        assertIsActive(tx2);
        assertFalse(latch1.isOpen());
        assertFalse(latch2.isOpen());
        assertHasListeners(ref, latch2, latch1);
    }

    @Test
    public void whenVersionAlreadyThere_thenLatchOpenedAndNotRegistered() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref);
        //do the update that will prevent adding the listener
        ref.inc(stm);

        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertTrue(latch.isOpen());
        assertHasNoListeners(ref);
    }
}

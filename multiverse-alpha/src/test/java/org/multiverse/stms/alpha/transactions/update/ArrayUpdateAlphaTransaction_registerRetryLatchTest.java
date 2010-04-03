package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Latch;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.exceptions.SpeculativeConfigurationFailure;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.SpeculativeConfiguration;
import org.multiverse.utils.latches.CheapLatch;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasListeners;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasNoListeners;

public class ArrayUpdateAlphaTransaction_registerRetryLatchTest {

    private AlphaStmConfig stmConfig;
    private AlphaStm stm;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public AlphaTransaction startSutTransaction() {
        UpdateConfiguration config =
                new UpdateConfiguration(stmConfig.clock);
        return new ArrayUpdateAlphaTransaction(config, 100);
    }

    public AlphaTransaction startSutTransactionWithoutAutomaticReadTracking(SpeculativeConfiguration speculativeConfig) {
        UpdateConfiguration config = new UpdateConfiguration(stmConfig.clock)
                .withSpeculativeConfiguration(speculativeConfig)
                .withAutomaticReadTracking(false);

        return new ArrayUpdateAlphaTransaction(config, speculativeConfig.getMaximumArraySize());
    }

    @Test
    public void whenNoAutomaticReadtracking_thenNoRetryPossibleException() {
        ManualRef ref = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, false, false, 100);
        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking(speculativeConfig);
        tx.openForWrite(ref);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertFalse(latch.isOpen());
    }

    @Test
    public void whenSpeculativeNoAutomaticReadtracking_thenSpeculativeConfigurationFailure() {
        ManualRef ref = new ManualRef(stm);

        SpeculativeConfiguration speculativeConfig = new SpeculativeConfiguration(false, true, false, 100);
        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking(speculativeConfig);
        tx.openForWrite(ref);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (SpeculativeConfigurationFailure expected) {
        }

        assertFalse(latch.isOpen());
        assertTrue(speculativeConfig.isAutomaticReadTracking());
    }

    @Test
    public void whenUnused_thenNoRetryPossibleException() {
        AlphaTransaction tx = startSutTransaction();

        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertFalse(latch.isOpen());
        assertIsActive(tx);
    }

    @Test
    public void whenOnlyFresh_thenNoRetryPossibleException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        tx.openForConstruction(ref);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertHasNoListeners(ref);
        assertFalse(latch.isOpen());
        assertIsActive(tx);
    }

    @Test
    public void whenOpenedForWrite_thenListenerAppended() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);

        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
    }

    @Test
    public void whenOpenedForRead_thenListenerAppended() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);

        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
    }

    @Test
    public void whenListenerAlreadyExist_listenerAppended() {
        ManualRef ref = new ManualRef(stm);

        Latch latch1 = new CheapLatch();
        Latch latch2 = new CheapLatch();

        AlphaTransaction tx1 = startSutTransaction();
        tx1.openForRead(ref);
        tx1.registerRetryLatch(latch1);

        AlphaTransaction tx2 = startSutTransaction();
        tx2.openForRead(ref);
        tx2.registerRetryLatch(latch2);

        assertIsActive(tx2);
        assertFalse(latch1.isOpen());
        assertFalse(latch2.isOpen());
        assertHasListeners(ref, latch2, latch1);
    }

    @Test
    public void whenMultipleObjectsTooListenToo_thenListenerIsAddedTooAllObjects() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.openForRead(ref3);

        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
        assertHasListeners(ref3, latch);
    }

    @Test
    public void whenListenVersionAlreadyIsThere_thenLatchIsOpenedAndNothingIsRegistered() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);

        //do the desired update
        ref.inc(stm);

        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertTrue(latch.isOpen());
        assertHasNoListeners(ref);
    }
}


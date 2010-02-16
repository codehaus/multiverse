package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Latch;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.latches.CheapLatch;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasListeners;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasNoListeners;

public class TinyUpdateAlphaTransaction_registerRetryLatchTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public TinyUpdateAlphaTransaction startSutTransaction() {
        TinyUpdateAlphaTransaction.Config config = new TinyUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount,
                stmConfig.commitLockPolicy, true, optimalSize, true, true, true, true);
        return new TinyUpdateAlphaTransaction(config);
    }

    public TinyUpdateAlphaTransaction startSutTransactionWithoutAutomaticReadTracking() {
        TinyUpdateAlphaTransaction.Config config = new TinyUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount,
                stmConfig.commitLockPolicy, true, optimalSize, false, true, true, false);
        return new TinyUpdateAlphaTransaction(config);
    }

    @Test
    public void whenNullLatch_thenNullPointerException() {
        AlphaTransaction tx = startSutTransaction();

        try {
            tx.registerRetryLatch(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenNoAutomaticReadtracking_thenNoRetryPossibleException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransactionWithoutAutomaticReadTracking();
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
    public void whenUnused_thenNoRetryPossibleException() {
        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
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

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);

        Latch latch = new CheapLatch();
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
    }

    @Test
    public void whenFresh_thenNoRetryPossibleException() {
        ManualRef ref = ManualRef.createUncommitted();

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);
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
    public void whenVersionAlreadyThere_thenLatchOpenedAndNotRegistered() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransaction();
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

package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasListeners;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasNoListeners;

public class MonoReadonlyAlphaTransaction_registerRetryLatchTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createFastConfig();
        stm = new AlphaStm(stmConfig);
    }

    public MonoReadonlyAlphaTransaction startSutTransaction() {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock);
        return new MonoReadonlyAlphaTransaction(config);
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
    public void whenLoadedForRead_thenListenerAdded() {
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

    @Test
    public void integrationTest() {
        ManualRef ref = new ManualRef(stm);

        Latch latch = new CheapLatch();
        AlphaTransaction tx = startSutTransaction();
        tx.openForRead(ref);
        tx.registerRetryLatch(latch);

        WaitThread waitThread = new WaitThread(latch);
        waitThread.start();

        sleepSome();
        assertAlive(waitThread);

        ref.inc(stm);
        sleepSome();

        joinAll(waitThread);
    }

    class WaitThread extends TestThread {
        final Latch latch;

        WaitThread(Latch latch) {
            super("WaitThread");
            this.latch = latch;
        }

        @Override
        public void doRun() throws Exception {
            latch.awaitUninterruptible();
        }
    }
}

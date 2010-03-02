package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.OptimalSize;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

import static org.junit.Assert.*;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasListeners;
import static org.multiverse.stms.alpha.transactions.AlphaTransactionTestUtils.assertHasNoListeners;

public class FixedReadonlyAlphaTransaction_registerRetryLatchTest {
    private AlphaStm stm;
    private AlphaStmConfig stmConfig;
    private OptimalSize optimalSize;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        optimalSize = new OptimalSize(1);
    }

    public FixedReadonlyAlphaTransaction startTransactionUnderTest(int size) {
        optimalSize.set(size);

        FixedReadonlyAlphaTransaction.Config config = new FixedReadonlyAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.maxRetryCount, true, optimalSize, size);

        return new FixedReadonlyAlphaTransaction(config, size);
    }

    @Test
    public void whenNoReads_thenNoRetryPossibleException() {
        AlphaTransaction tx = startTransactionUnderTest(10);
        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertFalse(latch.isOpen());
    }

    @Test
    public void whenFirstListener_listenerAdded() {
        ManualRef ref = new ManualRef(stm);
        Latch latch = new CheapLatch();

        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.openForRead(ref);
        tx.registerRetryLatch(latch);

        assertFalse(latch.isOpen());
        assertHasListeners(ref, latch);
    }

    @Test
    public void whenMultipleListenersAvailable_thenLatchIsRegisteredTooAll() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);

        Latch latch = new CheapLatch();
        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.openForRead(ref3);
        tx.registerRetryLatch(latch);

        assertFalse(latch.isOpen());
        assertHasListeners(ref1, latch);
        assertHasListeners(ref2, latch);
        assertHasListeners(ref3, latch);
    }

    @Test
    public void whenCorrectVersionFound_thenRegistrationAbortedAndLatchOpened() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);
        ManualRef ref3 = new ManualRef(stm);

        Latch latch = new CheapLatch();
        AlphaTransaction tx = startTransactionUnderTest(10);
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.openForRead(ref3);

        //desired write
        ref2.inc(stm);

        tx.registerRetryLatch(latch);
        assertTrue(latch.isOpen());
        assertHasListeners(ref1, latch);
        assertHasNoListeners(ref2);
        assertHasNoListeners(ref3);
    }

    @Test
    public void whenPreexistingListener_thenListenerAppended() {
        ManualRef ref = new ManualRef(stm);
        Latch latch1 = new CheapLatch();
        Latch latch2 = new CheapLatch();

        AlphaTransaction tx1 = startTransactionUnderTest(10);
        tx1.openForRead(ref);
        tx1.registerRetryLatch(latch1);

        AlphaTransaction tx2 = startTransactionUnderTest(10);
        tx2.openForRead(ref);
        tx2.registerRetryLatch(latch2);

        assertFalse(latch1.isOpen());
        assertFalse(latch2.isOpen());
        assertHasListeners(ref, latch2, latch1);
    }
}

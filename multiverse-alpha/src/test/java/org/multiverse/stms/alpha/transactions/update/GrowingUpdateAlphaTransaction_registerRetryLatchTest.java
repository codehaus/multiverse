package org.multiverse.stms.alpha.transactions.update;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Latch;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.utils.Listeners;
import org.multiverse.utils.latches.CheapLatch;

import java.util.LinkedList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class GrowingUpdateAlphaTransaction_registerRetryLatchTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
    }

    public GrowingUpdateAlphaTransaction startSutTransaction() {
        GrowingUpdateAlphaTransaction.Config config = new GrowingUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                false, true, true, true, true);
        return new GrowingUpdateAlphaTransaction(config);
    }

    public GrowingUpdateAlphaTransaction startSutTransactionWithoutReadTracking() {
        GrowingUpdateAlphaTransaction.Config config = new GrowingUpdateAlphaTransaction.Config(
                stmConfig.clock,
                stmConfig.backoffPolicy,
                null,
                stmConfig.profiler,
                stmConfig.commitLockPolicy,
                stmConfig.maxRetryCount,
                false, true, true, true, false);
        return new GrowingUpdateAlphaTransaction(config);
    }

    private List<Latch> getLatches(ManualRef ref) {
        List<Latch> result = new LinkedList<Latch>();

        Listeners listeners = ref.___getListeners();
        while (listeners != null) {
            result.add(listeners.getListener());
            listeners = listeners.getNext();
        }

        return result;
    }

    @Test
    public void whenNoAutomaticReadtracking_thenNoRetryPossibleException() {
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = startSutTransactionWithoutReadTracking();
        tx.openForWrite(ref);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertFalse(latch.isOpen());
    }

    // =================== unit tests =======================================

    @Test
    public void whenFirstNoListeners_listenerAdded() {
        ManualRef ref = new ManualRef(stm);

        Latch latch = new CheapLatch();

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);
        tx.registerRetryLatch(latch);

        List<Latch> listeners = getLatches(ref);
        assertEquals(asList(latch), listeners);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenDesiredUpdateAlreadyExecuted_thenListenerOpened() {
        ManualRef ref = new ManualRef(stm);

        Latch latch = new CheapLatch();

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref);

        //update the prevents registration
        ref.inc(stm);

        tx.registerRetryLatch(latch);
        assertTrue(latch.isOpen());
        assertNull(ref.___getListeners());
    }

    @Test
    public void whenMultipleOpenForWrites_listenerAddedToEach() {
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        Latch latch = new CheapLatch();

        AlphaTransaction tx = startSutTransaction();
        tx.openForWrite(ref1);
        tx.openForWrite(ref2);
        tx.registerRetryLatch(latch);

        List<Latch> listeners1 = getLatches(ref1);
        assertEquals(asList(latch), listeners1);
        assertFalse(latch.isOpen());

        List<Latch> listeners2 = getLatches(ref2);
        assertEquals(asList(latch), listeners2);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenAlreadyContainsListener_thenListenerIsAppended() {
        ManualRef ref = new ManualRef(stm);

        Latch latch1 = new CheapLatch();

        AlphaTransaction tx1 = startSutTransaction();
        tx1.openForWrite(ref);
        tx1.registerRetryLatch(latch1);

        Latch latch2 = new CheapLatch();
        AlphaTransaction tx2 = startSutTransaction();
        tx2.openForWrite(ref);
        tx2.registerRetryLatch(latch2);

        List<Latch> latches = getLatches(ref);
        assertEquals(asList(latch2, latch1), latches);
        assertFalse(latch1.isOpen());
        assertFalse(latch2.isOpen());
    }

    @Test
    public void whenLatchNull_thenNullPointerException() {
        AlphaTransaction tx = startSutTransaction();

        try {
            tx.registerRetryLatch(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenUnused_thenNoRetryPossibleException() {
        Transaction tx = startSutTransaction();

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertIsActive(tx);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenOnlyFreshObjects_thenNoRetryPossibleException() {
        AlphaTransaction tx = startSutTransaction();

        ManualRef ref = new ManualRef(tx, 0);

        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertFalse(latch.isOpen());
        assertIsActive(tx);
    }

    @Test
    public void integrationTest_abortAndRetryOnSingleObject() {
        final ManualRef ref = new ManualRef(stm, 0);

        TestThread thread = new TestThread() {
            public void doRun() {
                TransactionFactory factory = stm.getTransactionFactoryBuilder()
                        .setAutomaticReadTracking(true).build();

                new TransactionTemplate(factory) {
                    @Override
                    public Object execute(Transaction t) throws Exception {
                        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
                        if (ref.get(tx) == 0) {
                            retry();
                        }
                        return null;
                    }
                }.execute();
            }
        };

        thread.start();
        sleepMs(300);
        assertTrue(thread.isAlive());

        ref.set(stm, 1);
        sleepMs(300);

        joinAll(thread);
    }

    @Test
    public void integrationTest_abortAndRetryOnMultipleObjects() {
        final ManualRef ref1 = new ManualRef(stm, 0);
        final ManualRef ref2 = new ManualRef(stm, 0);
        final ManualRef ref3 = new ManualRef(stm, 0);

        TestThread thread = new TestThread() {
            public void doRun() {
                TransactionFactory factory = stm.getTransactionFactoryBuilder().setAutomaticReadTracking(true).build();
                new TransactionTemplate(factory) {
                    @Override
                    public Object execute(Transaction t) throws Exception {
                        AlphaTransaction tx = (AlphaTransaction) getThreadLocalTransaction();
                        if (ref1.get(tx) == 0 && ref2.get(tx) == 0 && ref3.get(tx) == 0) {
                            retry();
                        }
                        return null;
                    }
                }.execute();
            }
        };

        thread.start();
        sleepMs(300);
        assertTrue(thread.isAlive());

        ref2.set(stm, 1);
        sleepMs(300);

        joinAll(thread);
    }
}

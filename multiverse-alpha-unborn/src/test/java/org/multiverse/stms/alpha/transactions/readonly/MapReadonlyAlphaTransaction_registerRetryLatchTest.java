package org.multiverse.stms.alpha.transactions.readonly;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Listeners;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaStmConfig;
import org.multiverse.stms.alpha.manualinstrumentation.ManualRef;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class MapReadonlyAlphaTransaction_registerRetryLatchTest {

    private AlphaStm stm;
    private AlphaStmConfig stmConfig;

    @Before
    public void setUp() {
        stmConfig = AlphaStmConfig.createDebugConfig();
        stm = new AlphaStm(stmConfig);
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    public MapReadonlyAlphaTransaction createSutTransaction() {
        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true);
        return new MapReadonlyAlphaTransaction(config);
    }

    @Test
    public void whenExplicitRetryNotAllowed_thenNoRetryPossibleException() {
        ManualRef ref = new ManualRef(stm);

        ReadonlyConfiguration config = new ReadonlyConfiguration(stmConfig.clock, true)
                .withExplicitRetryAllowed(false);

        AlphaTransaction tx = new MapReadonlyAlphaTransaction(config);
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
    public void whenEmptyReadSet_thenNoRetryPossibleException() {
        Latch latch = new CheapLatch();

        AlphaTransaction tx = createSutTransaction();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertFalse(latch.isOpen());
        assertIsActive(tx);
    }

    @Test
    public void whenVersionNewer_thenLatchOpened() {
        ManualRef ref = new ManualRef(stm);
        Latch latch = new CheapLatch();

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref);

        ref.inc(stm);
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertTrue(latch.isOpen());
        assertNull(ref.___getListeners());
    }

    @Test
    public void whenSingletonReadSet_thenLatchIsRegistered() {
        Latch latch = new CheapLatch();
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref);
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());

        Listeners listeners = ref.___getListeners();
        assertNotNull(listeners);
        assertSame(latch, listeners.getListener());
        assertNull(listeners.getNext());
    }

    @Test
    public void whenMultipleItemsRead_thenLatchIsRegisteredToAll() {
        Latch latch = new CheapLatch();
        ManualRef ref1 = new ManualRef(stm);
        ManualRef ref2 = new ManualRef(stm);

        AlphaTransaction tx = createSutTransaction();
        tx.openForRead(ref1);
        tx.openForRead(ref2);
        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());

        Listeners listeners1 = ref1.___getListeners();
        assertNotNull(listeners1);
        assertSame(latch, listeners1.getListener());
        assertNull(listeners1.getNext());

        Listeners listener2 = ref2.___getListeners();
        assertNotNull(listener2);
        assertSame(latch, listener2.getListener());
        assertNull(listener2.getNext());
    }

    @Test
    public void whenAlreadyContainsListener_newListenerAppended() {
        Latch oldLatch = new CheapLatch();
        Latch newLatch = new CheapLatch();
        ManualRef ref = new ManualRef(stm);

        AlphaTransaction tx1 = createSutTransaction();
        tx1.openForRead(ref);
        tx1.registerRetryLatch(oldLatch);

        AlphaTransaction tx2 = createSutTransaction();
        tx2.openForRead(ref);
        tx2.registerRetryLatch(newLatch);

        assertIsActive(tx2);
        assertFalse(newLatch.isOpen());

        Listeners listeners = ref.___getListeners();
        assertNotNull(listeners);
        assertSame(newLatch, listeners.getListener());
        assertNotNull(listeners.getNext());
        assertSame(oldLatch, listeners.getNext().getListener());
        assertNull(listeners.getNext().getNext());
    }

    @Test
    public void integrationTest() {
        SomeRef ref = new SomeRef(0);

        AwaitThread thread1 = new AwaitThread(ref);
        AwaitThread thread2 = new AwaitThread(ref);
        startAll(thread1, thread2);

        sleepMs(100);

        assertTrue(thread1.isAlive());
        assertTrue(thread2.isAlive());

        ref.set(1);

        sleepMs(100);
        joinAll(thread1, thread2);
    }

    class AwaitThread extends TestThread {

        private final SomeRef ref;

        AwaitThread(SomeRef ref) {
            super("AwaitThread");
            this.ref = ref;
        }

        @Override
        public void doRun() throws Exception {
            ref.await(1);
        }
    }

    @TransactionalObject
    static class SomeRef {
        int value;

        SomeRef(int value) {
            this.value = value;
        }

        public void set(int newValue) {
            this.value = newValue;
        }

        @TransactionalMethod(readonly = true, trackReads = true)
        public void await(int value) {
            if (this.value != value) {
                retry();
            }
        }
    }
}

package org.multiverse.stms.alpha;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

public class UpdateAlphaTransaction_abortAndRegisterRetryLatchTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    public AlphaTransaction startUpdateTransaction() {
        AlphaTransaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }


    @Test
    public void callFailsIfTransactionIsStartedAndNullLatch() {
        AlphaTransaction t = startUpdateTransaction();

        try {
            t.abortAndRegisterRetryLatch(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(t);
    }

    @Test
    public void callFailsIfNothingHasBeenAttached() {
        Transaction t = startUpdateTransaction();

        Latch latch = new CheapLatch();
        try {
            t.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertIsAborted(t);
        assertFalse(latch.isOpen());
    }

    @Test
    public void callFailsIfOnlyNewObjects() {
        Transaction t = startUpdateTransaction();
        IntRef ref = new IntRef(0);

        Latch latch = new CheapLatch();
        try {
            t.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertFalse(latch.isOpen());
        assertIsAborted(t);
    }

    @Test
    public void abortAndRetryOnSingleObject() {
        final IntRef value = new IntRef(0);

        TestThread t = new TestThread() {
            @AtomicMethod
            public void doRun() {
                if (value.get() == 0) {
                    retry();
                }
            }
        };

        t.start();
        sleepMs(300);
        assertTrue(t.isAlive());

        value.set(1);
        sleepMs(300);

        joinAll(t);
    }

    @Test
    public void abortAndRetryOnMultipleObjects() {
        final IntRef value1 = new IntRef(0);
        final IntRef value2 = new IntRef(0);
        final IntRef value3 = new IntRef(0);

        TestThread t = new TestThread() {
            @AtomicMethod
            public void doRun() {
                if (value1.get() == 0 && value2.get() == 0 && value3.get() == 0) {
                    retry();
                }
            }
        };

        t.start();
        sleepMs(300);
        assertTrue(t.isAlive());

        value2.set(1);
        sleepMs(300);

        joinAll(t);
    }

    @Test
    public void callFailsIfTransactionIsCommitted() {
        Transaction t = startUpdateTransaction();
        t.commit();

        long expectedVersion = stm.getTime();

        try {
            t.abortAndRegisterRetryLatch(null);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(t);
        assertEquals(expectedVersion, stm.getTime());
    }

    @Test
    public void callFailsIfTransactionIsAborted() {
        Transaction t = startUpdateTransaction();
        t.abort();

        long expectedVersion = stm.getTime();
        Latch latch = new CheapLatch();

        try {
            t.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
        assertEquals(expectedVersion, stm.getTime());
        assertFalse(latch.isOpen());
    }
}

package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.Transaction;
import org.multiverse.stms.AbstractTransactionImpl;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.TestUtils.sleepMs;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class CountdownCommitBarrier_tryAwaitCommitTransactionWithTimeoutTest {
    private CountdownCommitBarrier barrier;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        barrier = new CountdownCommitBarrier(1);

        try {
            barrier.tryAwaitCommit(null, 1, TimeUnit.DAYS);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isOpen());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenNullTimeout_thenNullPointerException() {
        barrier = new CountdownCommitBarrier(1);

        Transaction tx = new AbstractTransactionImpl();
        try {
            barrier.tryAwaitCommit(tx, 1, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsActive(tx);
        assertTrue(barrier.isOpen());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    @Ignore
    public void whenNotLastOne(){

    }

    @Test
    @Ignore
    public void whenInterruptedWhileWaiting() {

    }

    @Test
    @Ignore
    public void whenTimeout() {

    }

    @Test
    @Ignore
    public void whenAbortedWhileWaiting() {

    }

    @Test
    @Ignore
    public void whenCommittedWhileWaiting() throws InterruptedException {
        barrier = new CountdownCommitBarrier(2);

        final TransactionalInteger ref = new TransactionalInteger();

        TestThread t = new TestThread(){
            @TransactionalMethod
            @Override
            public void doRun() throws Exception {
                ref.inc();
                Transaction tx = getThreadLocalTransaction();
                boolean result = barrier.tryAwaitCommit(tx, 1, TimeUnit.DAYS);
                assertTrue(result);
            }
        };

        t.start();
        sleepMs(500);

        barrier.awaitCommit();

        t.join();
        t.assertNothingThrown();
        assertTrue(barrier.isOpen());
        assertEquals(1, ref.get());
    }

    @Test
    public void whenAborted() {
        barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.tryAwaitCommit(tx, 1, TimeUnit.DAYS);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsActive(tx);
        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenCommitted() {
        barrier = new CountdownCommitBarrier(0);

        Transaction tx = new AbstractTransactionImpl();

        try {
            barrier.tryAwaitCommit(tx, 1, TimeUnit.DAYS);
            fail();
        } catch (IllegalStateException expected) {
        }

        assertIsActive(tx);
        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
    }
}

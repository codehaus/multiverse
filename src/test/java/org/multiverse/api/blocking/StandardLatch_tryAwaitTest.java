package org.multiverse.api.blocking;

import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public class StandardLatch_tryAwaitTest {

    @Test
    public void whenNullTimeUnit_thenNullPointerException() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();

        try {
            latch.tryAwait(era, 10, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(era, latch.getEra());
        assertClosed(latch);
    }

    @Test
    public void whenAlreadyOpenAndSameEra() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();
        latch.open(era);

        long result = latch.tryAwait(era, 10, TimeUnit.NANOSECONDS);

        assertEquals(10, result);
        assertOpen(latch);
        assertEquals(era, latch.getEra());
    }

    @Test
    public void whenAlreadyOpenAndDifferentEra() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        long oldEra = latch.getEra();
        latch.prepareForPooling();
        long era = latch.getEra();
        latch.open(era);

        long result = latch.tryAwait(oldEra, 10, TimeUnit.NANOSECONDS);

        assertEquals(10, result);
        assertOpen(latch);
        assertEquals(era, latch.getEra());
    }

    @Test
    public void whenClosedButDifferentEra() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();
        latch.prepareForPooling();

        long expectedEra = latch.getEra();
        long result = latch.tryAwait(era, 10, TimeUnit.NANOSECONDS);

        assertEquals(10, result);
        assertEquals(expectedEra, latch.getEra());
        assertClosed(latch);
    }

    @Test
    public void whenSomeWaitingIsNeeded() {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era, 10, TimeUnit.SECONDS);
        t.start();

        sleepMs(500);

        assertAlive(t);
        latch.open(era);

        joinAll(t);
        assertOpen(latch);
        //assertTrue()
    }

    @Test
    public void testAlreadyOpenAndNulTimeout() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();
        latch.open(era);

        long remaining = latch.tryAwait(era, 0, TimeUnit.NANOSECONDS);

        assertEquals(0, remaining);
        assertOpen(latch);
        assertEra(latch, era);
    }

    @Test
    public void whenStillClosedAndNulTimeout() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();

        long remaining = latch.tryAwait(era, 0, TimeUnit.NANOSECONDS);

        assertTrue(remaining < 0);
        assertClosed(latch);
        assertEra(latch, era);
    }

    @Test
    public void whenAlreadyOpenAndNegativeTimeout() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();
        latch.open(era);

        long remaining = latch.tryAwait(era, -10, TimeUnit.NANOSECONDS);

        assertTrue(remaining < 0);
        assertOpen(latch);
        assertEra(latch, era);
    }

    @Test
    public void whenStillClosedAndNegativeTimeout() throws InterruptedException {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();

        long remaining = latch.tryAwait(era, -10, TimeUnit.NANOSECONDS);

        assertTrue(remaining < 0);
        assertClosed(latch);
        assertEra(latch, era);
    }

    @Test
    public void whenTimeout() {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era, 1, TimeUnit.SECONDS);
        t.start();
        joinAll(t);

        assertClosed(latch);
        assertEra(latch, era);
        assertTrue(t.result < 0);
    }

    @Test
    public void whenStartingInterrupted() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        Thread.currentThread().interrupt();
        try {
            latch.await(era);
            fail();
        } catch (InterruptedException expected) {
        }

        assertEra(latch, era);
        assertClosed(latch);
    }

    @Test
    public void whenInterruptedWhileWaiting() {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era, 10, TimeUnit.SECONDS);
        t.setPrintStackTrace(false);
        t.start();

        sleepMs(500);

        assertAlive(t);
        t.interrupt();

        joinAll(t);
        assertClosed(latch);
        assertEra(latch, era);
        t.assertInterrupted();
    }

    @Test
    public void whenResetWhileWaiting_thenSleepingThreadsNotified() {
        StandardLatch latch = new StandardLatch();
        long era = latch.getEra();
        AwaitThread t = new AwaitThread(latch, era, 10, TimeUnit.SECONDS);
        t.start();

        sleepMs(500);
        assertAlive(t);

        latch.prepareForPooling();
        joinAll(t);

        assertClosed(latch);
        assertEra(latch, era + 1);
        assertTrue(t.result > 0);
        assertTrue(t.result < TimeUnit.SECONDS.toNanos(10));
    }

    class AwaitThread extends TestThread {
        private final Latch latch;
        private final long expectedEra;
        private long timeout;
        private TimeUnit unit;
        private long result;

        AwaitThread(Latch latch, long expectedEra, long timeout, TimeUnit unit) {
            this.latch = latch;
            this.expectedEra = expectedEra;
            this.timeout = timeout;
            this.unit = unit;
        }

        @Override
        public void doRun() throws Exception {
            result = latch.tryAwait(expectedEra, timeout, unit);
        }
    }
}

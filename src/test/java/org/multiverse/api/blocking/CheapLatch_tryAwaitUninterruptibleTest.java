package org.multiverse.api.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public class CheapLatch_tryAwaitUninterruptibleTest {

    @Before
    public void setUp() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void whenNullTimeUnit_thenNullPointerException() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        try {
            latch.tryAwaitUninterruptible(era, 10, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(era, latch.getEra());
        assertClosed(latch);
    }

    @Test
    public void whenAlreadyOpenAndSameEra() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.open(era);

        long result = latch.tryAwaitUninterruptible(era, 10, TimeUnit.NANOSECONDS);

        assertEquals(10, result);
        assertOpen(latch);
        assertEquals(era, latch.getEra());
    }

    @Test
    public void whenAlreadyOpenAndDifferentEra() {
        CheapLatch latch = new CheapLatch();
        long oldEra = latch.getEra();
        latch.prepareForPooling();
        long era = latch.getEra();
        latch.open(era);

        long result = latch.tryAwaitUninterruptible(oldEra, 10, TimeUnit.NANOSECONDS);

        assertEquals(10, result);
        assertOpen(latch);
        assertEquals(era, latch.getEra());
    }

    @Test
    public void whenClosedButDifferentEra() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.prepareForPooling();

        long expectedEra = latch.getEra();
        long result = latch.tryAwaitUninterruptible(era, 10, TimeUnit.NANOSECONDS);

        assertEquals(10, result);
        assertEquals(expectedEra, latch.getEra());
        assertClosed(latch);
    }

    @Test
    public void whenSomeWaitingIsNeeded() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era, 10, TimeUnit.SECONDS);
        t.start();

        sleepMs(500);

        assertAlive(t);
        latch.open(era);

        joinAll(t);
        assertOpen(latch);
    }

    @Test
    public void whenTimeout() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era, 1, TimeUnit.SECONDS);
        t.start();
        joinAll(t);

        assertClosed(latch);
        assertEra(latch, era);
        assertTrue(t.result < 0);
    }


    @Test
    public void testAlreadyOpenAndNulTimeout() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.open(era);

        long remaining = latch.tryAwaitUninterruptible(era, 0, TimeUnit.NANOSECONDS);

        assertEquals(0, remaining);
        assertOpen(latch);
        assertEra(latch, era);
    }

    @Test
    public void whenStillClosedAndNulTimeout() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        long remaining = latch.tryAwaitUninterruptible(era, 0, TimeUnit.NANOSECONDS);

        assertTrue(remaining < 0);
        assertClosed(latch);
        assertEra(latch, era);
    }

    @Test
    public void whenAlreadyOpenAndNegativeTimeout() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.open(era);

        long remaining = latch.tryAwaitUninterruptible(era, -10, TimeUnit.NANOSECONDS);

        assertTrue(remaining < 0);
        assertOpen(latch);
        assertEra(latch, era);
    }

    @Test
    public void whenStillClosedAndNegativeTimeout() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        long remaining = latch.tryAwaitUninterruptible(era, -10, TimeUnit.NANOSECONDS);

        assertTrue(remaining < 0);
        assertClosed(latch);
        assertEra(latch, era);
    }


    @Test
    public void whenStartingInterrupted() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era, 10, TimeUnit.SECONDS);
        t.setStartInterrupted(true);
        t.start();

        sleepMs(500);
        assertAlive(t);

        //do some waiting and see if it still is waiting
        sleepMs(500);
        assertAlive(t);

        //now lets open the latch
        latch.open(era);

        joinAll(t);
        assertOpen(latch);
        assertEra(latch, era);
        t.assertEndedWithInterruptStatus(true);

        assertTrue(t.result > 0);
        assertTrue(t.result < TimeUnit.SECONDS.toNanos(10));
    }

    @Test
    public void whenInterruptedWhileWaiting() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era, 10, TimeUnit.SECONDS);
        t.start();

        sleepMs(500);

        assertAlive(t);
        t.interrupt();

        //do some waiting and see if it still is waiting
        sleepMs(500);
        assertAlive(t);

        //now lets open the latch
        latch.open(era);

        joinAll(t);
        assertOpen(latch);
        assertEra(latch, era);
        t.assertEndedWithInterruptStatus(true);

        assertTrue(t.result > 0);
        assertTrue(t.result < TimeUnit.SECONDS.toNanos(10));
    }

    @Test
    public void whenResetWhileWaiting_thenSleepingThreadsNotified() {
        CheapLatch latch = new CheapLatch();
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
            result = latch.tryAwaitUninterruptible(expectedEra, timeout, unit);
        }
    }
}

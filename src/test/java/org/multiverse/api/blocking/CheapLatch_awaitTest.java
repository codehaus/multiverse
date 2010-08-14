package org.multiverse.api.blocking;

import org.junit.Test;
import org.multiverse.TestThread;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.benchmarks.BenchmarkUtils.joinAll;

public class CheapLatch_awaitTest {

    @Test
    public void whenAlreadyOpenAndSameEra() throws InterruptedException {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.open(era);

        latch.await(era);

        assertOpen(latch);
        assertEquals(era, latch.getEra());
    }

    @Test
    public void whenAlreadyOpenAndDifferentEra() throws InterruptedException {
        CheapLatch latch = new CheapLatch();
        long oldEra = latch.getEra();
        latch.prepareForPooling();
        long era = latch.getEra();
        latch.open(era);

        latch.await(oldEra);

        assertOpen(latch);
        assertEquals(era, latch.getEra());
    }

    @Test
    public void whenClosedButDifferentEra() throws InterruptedException {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.prepareForPooling();

        long expectedEra = latch.getEra();
        latch.await(era);

        assertEquals(expectedEra, latch.getEra());
        assertClosed(latch);
    }

    @Test
    public void whenSomeWaitingIsNeeded() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era);
        t.start();

        sleepMs(500);

        assertAlive(t);
        latch.open(era);

        joinAll(t);
        assertOpen(latch);
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
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era);
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
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        AwaitThread t = new AwaitThread(latch, era);
        t.start();

        sleepMs(500);
        assertAlive(t);

        latch.prepareForPooling();
        joinAll(t);

        assertClosed(latch);
        assertEra(latch, era + 1);
    }

    class AwaitThread extends TestThread {
        private final Latch latch;
        private final long expectedEra;


        AwaitThread(Latch latch, long expectedEra) {
            this.latch = latch;
            this.expectedEra = expectedEra;
        }

        @Override
        public void doRun() throws Exception {
            latch.await(expectedEra);
        }
    }
}

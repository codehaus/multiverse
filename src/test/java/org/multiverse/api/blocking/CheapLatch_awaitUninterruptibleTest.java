package org.multiverse.api.blocking;

import org.junit.Test;
import org.multiverse.TestThread;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.benchmarks.BenchmarkUtils.joinAll;

public class CheapLatch_awaitUninterruptibleTest {

    @Test
    public void whenAlreadyOpenAndSameEra() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.open(era);

        latch.awaitUninterruptible(era);

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

        latch.awaitUninterruptible(oldEra);

        assertOpen(latch);
        assertEquals(era, latch.getEra());
    }

    @Test
    public void whenClosedButDifferentEra() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();
        latch.prepareForPooling();

        long expectedEra = latch.getEra();
        latch.awaitUninterruptible(era);

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
    public void whenInterruptedWhileWaiting() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era);
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
    }


    @Test
    public void whenStartingInterrupted() {
        CheapLatch latch = new CheapLatch();
        long era = latch.getEra();

        AwaitThread t = new AwaitThread(latch, era);
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
            latch.awaitUninterruptible(expectedEra);
        }
    }
}

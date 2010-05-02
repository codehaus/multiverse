package org.multiverse.stms.alpha.integrationtests;

import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.api.programmatic.ProgrammaticLong;
import org.multiverse.api.programmatic.ProgrammaticReferenceFactory;
import org.multiverse.stms.alpha.AlphaStm;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.*;

/**
 * A synthetic test check check how independant transactions scale.
 */
public class IndependentScalabilityStressTest {
    private long updateCount = 10 * 1000 * 1000;

    @Test
    public void testWithSharedStm() {
        test(true);
    }

    @Test
    public void testNoSharedStm() {
        test(false);
    }

    private void test(boolean share) {
        int processors = Runtime.getRuntime().availableProcessors() / 2;

        Stm stm = null;

        long[] resultsInMs = new long[processors];
        for (int k = 0; k < processors; k++) {

            if (share) {
                if (stm == null) {
                    stm = AlphaStm.createFast();
                }
            } else {
                stm = AlphaStm.createFast();
            }

            resultsInMs[k] = test(stm, k + 1);
        }

        for (int k = 0; k < processors; k++) {
            int processorCount = k + 1;
            double transactionPerSecond = (updateCount * (processorCount * TimeUnit.SECONDS.toMillis(1))
                    / resultsInMs[k]);


            System.out.printf("%s processors took %s seconds %s transactions/second  speedupfactor %s\n",
                    processorCount,
                    resultsInMs[k],
                    format(transactionPerSecond),
                    format(processorCount * 1.0 * resultsInMs[0] / resultsInMs[k]));
        }
    }

    public long test(Stm stm, int threadCount) {
        System.out.println("--------------------------------------------------------");
        System.out.printf("starting with %s threads\n", threadCount);
        System.out.println("--------------------------------------------------------");

        Latch startLatch = new CheapLatch();
        TestThread[] threads = createThreads(stm, startLatch, threadCount);
        startAll(threads);
        long startMs = System.currentTimeMillis();
        startLatch.open();
        joinAll(threads);
        return System.currentTimeMillis() - startMs;
    }

    public MyThread[] createThreads(Stm stm, Latch startLatch, int threadCount) {
        MyThread[] threads = new MyThread[threadCount];

        ProgrammaticReferenceFactory factory = stm.getProgrammaticReferenceFactoryBuilder()
                .build();

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new MyThread(k, factory.atomicCreateLong(0), startLatch);
        }
        return threads;
    }

    class MyThread extends TestThread {
        private final ProgrammaticLong ref;
        private final Latch startLatch;

        public MyThread(int id, ProgrammaticLong ref, Latch startLatch) {
            super("Thread-" + id);
            this.startLatch = startLatch;
            this.ref = ref;
        }

        @Override
        public void doRun() {
            startLatch.awaitUninterruptible();

            for (int k = 0; k < updateCount; k++) {
                ref.inc(1);

                if (k % 2000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}


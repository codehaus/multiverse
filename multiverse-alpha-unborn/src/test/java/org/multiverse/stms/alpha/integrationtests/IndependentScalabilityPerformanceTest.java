package org.multiverse.stms.alpha.integrationtests;

import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;
import org.multiverse.api.Stm;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.api.programmatic.ProgrammaticLongRef;
import org.multiverse.stms.alpha.AlphaStm;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.*;

/**
 * A synthetic test check check how independant transactions scale.
 */
public class IndependentScalabilityPerformanceTest {
    private volatile boolean stop;

    @Test
    public void testWithSharedStm() {
        test(true);
    }

    @Test
    public void testNoSharedStm() {
        test(false);
    }

    private void test(boolean share) {
        int processorCount = Math.max(4, processorCount() / 2);

       TestResult[] results = new TestResult[processorCount];
        for (int k = 0; k < processorCount; k++) {
            results[k] = test(share, k + 1);
        }

        for (int k = 0; k < results.length; k++) {
            TestResult result = results[k];
            System.out.printf("%s processors took %s seconds, %s transactions/second, speedupfactor %s\n",
                    result.processorCount,
                    result.durationMs,
                    format(result.performance()),
                    format(result.performance() / results[0].performance()));
        }
    }

    public TestResult test(boolean share, int threadCount) {
        stop = false;

        System.out.println("--------------------------------------------------------");
        System.out.printf("starting with %s threads\n", threadCount);
        System.out.println("--------------------------------------------------------");

        Latch startLatch = new CheapLatch();
        ModifyThread[] threads = createThreads(share, startLatch, threadCount);
        startAll(threads);

        long startMs = System.currentTimeMillis();
        startLatch.open();
        sleepMs(TestUtils.getStressTestDurationMs(10 * 1000));
        stop = true;
        joinAll(threads);
        return new TestResult(System.currentTimeMillis() - startMs, sum(threads), threadCount);
    }

    class TestResult {
        final long transactionCount;
        final long durationMs;
        final int processorCount;

        TestResult(long durationMs, long transactionCount, int processorCount) {
            this.durationMs = durationMs;
            this.transactionCount = transactionCount;
            this.processorCount = processorCount;
        }

        double performance() {
            return (transactionCount * (TimeUnit.SECONDS.toMillis(1))) / durationMs;
        }
    }

    public ModifyThread[] createThreads(boolean share, Latch startLatch, int threadCount) {
        ModifyThread[] threads = new ModifyThread[threadCount];

        Stm stm = null;
        for (int k = 0; k < threads.length; k++) {
            if (share) {
                if(stm == null){
                    stm = AlphaStm.createFast();
                }
            } else {
                 stm = AlphaStm.createFast();
            }
            threads[k] = new ModifyThread(
                    k,
                    stm.getProgrammaticRefFactoryBuilder().build().atomicCreateLongRef(0), 
                    startLatch);
        }
        return threads;
    }

    long sum(ModifyThread[] threads) {
        long result = 0;
        for (ModifyThread t : threads) {
            result += t.incCount;
        }
        return result;
    }

    class ModifyThread extends TestThread {
        private final ProgrammaticLongRef ref;
        private final Latch startLatch;
        private long incCount;

        public ModifyThread(int id, ProgrammaticLongRef ref, Latch startLatch) {
            super("Thread-" + id);
            this.startLatch = startLatch;
            this.ref = ref;
        }

        @Override
        public void doRun() {
            startLatch.awaitUninterruptible();

            while (!stop) {
                ref.inc(1);
                incCount++;

                if (incCount % 10000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), incCount);
                }
            }
        }
    }
}


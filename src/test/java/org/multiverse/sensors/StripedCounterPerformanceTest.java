package org.multiverse.sensors;

import org.junit.Test;
import org.multiverse.TestThread;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

/**
 * IterationCount is set to a low value because the counter still isn't scalable.
 * Still work in progress
 */
public class StripedCounterPerformanceTest {

    private long iterationCount = 1000 * 1000;
    private static final int STRIPE_LENGTH = 8000;

    @Test
    public void when1Thread() {
        test(1, STRIPE_LENGTH);
    }

    @Test
    public void when2Threads() {
        test(2, STRIPE_LENGTH);
    }

    @Test
    public void when4Threads() {
        test(4, STRIPE_LENGTH);
    }

    @Test
    public void when6Threads() {
        test(6, STRIPE_LENGTH);
    }

    @Test
    public void when8Threads() {
        test(8, STRIPE_LENGTH);
    }

    public void test(int threadCount, int stripeLength) {
        System.out.println("=======================================================");
        StripedCounter counter = new StripedCounter(stripeLength);


        StressThread[] threads = new StressThread[threadCount];
        Object[] counters = new Object[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new StressThread(k, counter);
        }

        long startMs = System.currentTimeMillis();
        startAll(threads);
        long totalDurationMs = joinAll(threads);
        long durationMs = System.currentTimeMillis() - startMs;

        long count = 0;
        for (StressThread thread : threads) {
            count += thread.count;
        }

        System.out.printf("Duration:               %s ms\n", format(durationMs));
        System.out.printf("Thread count:           %s\n", threadCount);
        System.out.printf("iterations per thread:  %s\n", iterationCount);
        System.out.printf("Performance:            %s\n",
                transactionsPerSecondAsString(iterationCount, totalDurationMs, threadCount));
        System.out.printf("Performance per thread: %s\n",
                transactionsPerSecondPerThreadAsString(iterationCount, totalDurationMs, threadCount));

        assertEquals(count, counter.get());
    }

    class StressThread extends TestThread {
        private final StripedCounter counter;
        private long count;
        private final int id;


        StressThread(int id, StripedCounter counter) {
            super("StressThread-" + id);
            this.counter = counter;
            this.id = id;
        }

        @Override
        public void doRun() throws Exception {
            for (long k = 0; k < iterationCount; k++) {
                long value = k % 100;
                count += value;
                counter.incAtIndex(id, value);
            }
        }
    }
}

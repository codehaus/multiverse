package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class UnsharedAtomicLongPerformanceTest {

    private volatile boolean stop;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stop = false;
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test_1Threads() {
        test(1);
    }

    @Test
    public void test_2Threads() {
        test(2);
    }

    @Test
    public void test_4Threads() {
        test(4);
    }

    @Test
    public void test_8Threads() {
        test(8);
    }

    @Test
    public void test_16Threads() {
        test(16);
    }

    @Test
    public void test_32Threads() {
        test(32);
    }

    public void test(int threadCount) {
        AtomicIncThread[] threads = createThreads(threadCount);

        long startNs = System.nanoTime();
        startAll(threads);
        sleepMs(getStressTestDurationMs(20 * 1000));
        stop = true;
        joinAll(threads);

        long totalIncCount = sum(threads);
        assertEquals(totalIncCount, sum(threads));

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * totalIncCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s increments/second\n", format(transactionsPerSecond));
        System.out.printf("Performance %s increments/second per thread\n",
                format(transactionsPerSecond/threadCount));
    }

    private long sum(AtomicIncThread[] threads) {
        long result = 0;
        for (AtomicIncThread thread : threads) {
            result += thread.count;
        }
        return result;
    }

    private AtomicIncThread[] createThreads(int threadCount) {
        AtomicIncThread[] threads = new AtomicIncThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new AtomicIncThread(k);
        }
        return threads;
    }

    public class AtomicIncThread extends TestThread {
        private final AtomicLong ref = new AtomicLong();
        private long count;

        public AtomicIncThread(int id) {
            super("AtomicIncThread-" + id);
        }

        public long get() {
            return ref.get();
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                ref.incrementAndGet();

                if (count % (100 * 1000 * 1000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
                count++;
            }
        }
    }
}

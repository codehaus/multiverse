package org.multiverse.transactional.refs;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class LongRefPerformanceTest {

    private int threadCount;
    private long incCountPerThread = 1000 * 1000 * 100;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test_1() {
        test(1);
    }

    @Test
    public void test_2() {
        test(2);
    }

    @Test
    public void test_4() {
        test(4);
    }

    @Test
    public void test_8() {
        test(8);
    }

    @Test
    public void test_16() {
        test(16);
    }

    public void test(int threadCount) {
        this.threadCount = threadCount;
        AtomicIncThread[] threads = createThreads();

        long startNs = System.nanoTime();
        startAll(threads);
        joinAll(threads);

        long totalIncCount = threadCount * incCountPerThread;
        assertEquals(totalIncCount, sum(threads));

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * totalIncCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));

    }

    private long sum(AtomicIncThread[] threads) {
        long result = 0;
        for (AtomicIncThread thread : threads) {
            result += thread.ref.get();
        }
        return result;
    }

    private AtomicIncThread[] createThreads() {
        AtomicIncThread[] threads = new AtomicIncThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new AtomicIncThread(k);
        }
        return threads;
    }

    public class AtomicIncThread extends TestThread {
        private final LongRef ref = new LongRef();

        public AtomicIncThread(int id) {
            super("AtomicIncThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < incCountPerThread; k++) {
                ref.inc(1);

                if (k % (1000 * 1000 * 10) == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}

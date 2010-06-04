package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.TestUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_sharedAtomicLongStressTest {


    private final AtomicLong ref = new AtomicLong();

    private int threadCount = 4;
    private volatile boolean stop;
    private boolean strict = false;

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
    public void test() {
        AtomicIncThread[] threads = createThreads();

        long startNs = System.nanoTime();
        startAll(threads);
        sleepMs(TestUtils.getStressTestDurationMs(60 * 1000));
        stop = true;
        joinAll(threads);

        long totalIncCount = sum(threads);

        long durationNs = System.nanoTime() - startNs;
        double transactionsPerSecond = (1.0d * totalIncCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s transactions/second\n", format(transactionsPerSecond));

    }

    private long sum(AtomicIncThread[] threads) {
        long result = 0;
        for (AtomicIncThread thread : threads) {
            result += thread.count;
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

        private long count;

        public AtomicIncThread(int id) {
            super("AtomicIncThread-" + id);
        }

        public long get() {
            return ref.get();
        }

        @Override
        public void doRun() throws Exception {
            while(!stop){
                if (strict) {
                    ref.incrementAndGet();
                } else {
                    long get = ref.get();
                    ref.compareAndSet(get, get + 1);
                }

                if (count % (10 * 1000 * 1000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
                count++;
            }
        }
    }

}

package org.multiverse.stms.alpha.programmatic;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.programmatic.ProgrammaticLongRef;
import org.multiverse.stms.alpha.AlphaStm;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaProgrammaticLongRef_atomicNothingSharedStressTest {

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
    public void test_1Thread() {
        test(1);
    }

    @Test
    public void test_2Thread() {
        test(2);
    }

    @Test
    public void test_4Thread() {
        test(4);
    }

    @Test
    public void test_8Thread() {
        test(8);
    }

    @Test
    public void test_16Thread() {
        test(16);
    }

    @Test
    public void test_32Thread() {
        test(32);
    }

    public void test(int threadCount) {
        AtomicIncThread[] threads = createThreads(threadCount);

        long startNs = System.nanoTime();
        startAll(threads);
        sleepMs(getStressTestDurationMs(30 * 1000));
        stop = true;
        joinAll(threads);

        long totalIncCount = sum(threads);
        assertEquals(totalIncCount, sum(threads));

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

    private AtomicIncThread[] createThreads(int threadCount) {
        AtomicIncThread[] threads = new AtomicIncThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            threads[k] = new AtomicIncThread(k);
        }
        return threads;
    }

    public class AtomicIncThread extends TestThread {
        private AlphaStm stm;
        private ProgrammaticLongRef ref;
        private long count;

        public AtomicIncThread(int id) {
            super("AtomicIncThread-" + id);

            stm = AlphaStm.createFast();
            System.out.println(stm.getClock().getClass());
            ref = stm.getProgrammaticRefFactoryBuilder()
                    .build()
                    .atomicCreateLongRef(0);
        }

        @Override
        public void doRun() throws Exception {
            while (!stop) {
                ref.atomicInc(1);

                if (count % (1000 * 1000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), count);
                }
                count++;
            }
        }
    }
}

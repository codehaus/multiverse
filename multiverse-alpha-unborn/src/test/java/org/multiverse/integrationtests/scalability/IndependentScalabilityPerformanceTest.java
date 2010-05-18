package org.multiverse.integrationtests.scalability;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.integrationtests.Ref;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * todo:
 * Test doesn't provide any value atm.
 */
public class IndependentScalabilityPerformanceTest {
    private Stm stm;
    private long updateCount = 5 * 1000 * 1000;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void test() {
        int processors = Runtime.getRuntime().availableProcessors();

        long[] resultsInMs = new long[processors];
        for (int k = 0; k < processors; k++) {
            resultsInMs[k] = test(k + 1);
        }

        for (int k = 0; k < processors; k++) {
            System.out.printf("%s processors took %s seconds\n", k + 1, resultsInMs[k]);
        }
    }

    public long test(int threadCount) {
        System.out.println("--------------------------------------------------------");
        System.out.printf("starting with %s threads\n", threadCount);
        System.out.println("--------------------------------------------------------");

        Latch startLatch = new CheapLatch();
        TestThread[] threads = createThreads(startLatch, threadCount);
        startAll(threads);
        long startMs = System.currentTimeMillis();
        startLatch.open();
        joinAll(threads);
        return System.currentTimeMillis() - startMs;
    }

    public MyThread[] createThreads(Latch startLatch, int threadCount) {
        MyThread[] threads = new MyThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new MyThread(k, new Ref(0), startLatch);
        }
        return threads;
    }

    class MyThread extends TestThread {
        private final Ref ref;
        private final Latch startLatch;

        public MyThread(int id, Ref ref, Latch startLatch) {
            super("Thread-" + id);
            this.startLatch = startLatch;
            this.ref = ref;
        }

        @Override
        public void doRun() {
            startLatch.awaitUninterruptible();

            for (int k = 0; k < updateCount; k++) {
                ref.inc();

                if (k % 5000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }
        }
    }
}

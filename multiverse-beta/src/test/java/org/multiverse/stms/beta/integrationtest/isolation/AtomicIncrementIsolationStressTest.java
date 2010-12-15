package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static java.lang.System.currentTimeMillis;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondPerThreadAsString;

public class AtomicIncrementIsolationStressTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void test() {
        int threadCount = 2;
        UpdateThread[] threads = new UpdateThread[threadCount];
        BetaLongRef ref = newLongRef(stm);
        long transactionsPerThread = 500 * 1000 * 1000;

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, ref, transactionsPerThread);
        }

        for (UpdateThread thread : threads) {
            thread.start();
        }

        joinAll(10 * 60, threads);
        long totalDurationMs = 0;
        for (UpdateThread thread : threads) {
            totalDurationMs += thread.durationMs;
        }

        System.out.println("--------------------------------------------------------");
        System.out.printf("Threadcount:       %s\n", threadCount);
        System.out.printf("Performance:       %s transactions/second/thread\n",
                transactionsPerSecondPerThreadAsString(transactionsPerThread, totalDurationMs, threadCount));
        System.out.printf("Performance:       %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        assertEquals(threadCount * transactionsPerThread, ref.atomicGet());
        System.out.println("ref.orec: " + ref.___toOrecString());
    }

    class UpdateThread extends TestThread {
        private final BetaLongRef ref;
        private final long count;
        private long durationMs;

        public UpdateThread(int id, BetaLongRef ref, long count) {
            super("UpdateThread-" + id);
            this.ref = ref;
            this.count = count;
        }

        @Override
        public void doRun() {
            long startMs = currentTimeMillis();

            for (long k = 0; k < count; k++) {
                ref.atomicIncrementAndGet(1);

                if (k % 10000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
            }

            durationMs = currentTimeMillis() - startMs;

            System.out.printf("finished %s after %s ms\n", getName(), durationMs);
        }
    }
}

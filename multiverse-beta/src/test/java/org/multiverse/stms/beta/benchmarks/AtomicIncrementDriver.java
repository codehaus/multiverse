package org.multiverse.stms.beta.benchmarks;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

public class AtomicIncrementDriver extends BenchmarkDriver implements BetaStmConstants {

    private transient BetaStm stm;
    private transient IncThread[] threads;
    private int threadCount;
    private long transactionsPerThread;
    private boolean sharedRef;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Threadcount %s\n", threadCount);
        System.out.printf("Multiverse > Transactions/Thread %s \n", transactionsPerThread);
        System.out.printf("Multiverse > SharedRef %s \n", sharedRef);

        stm = new BetaStm();
        threads = new IncThread[threadCount];
        BetaLongRef ref = sharedRef ? newLongRef(stm) : null;
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new IncThread(k, ref == null ? newLongRef(stm) : ref);
        }
    }

    @Override
    public void run(TestCaseResult testCaseResult) {
        startAll(threads);
        joinAll(threads);
    }

    @Override
    public void processResults(TestCaseResult testCaseResult) {
        long totalDurationMs = 0;
        for (IncThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);
        double transactionsPerSecond = transactionsPerSecond(
                transactionsPerThread, totalDurationMs, threadCount);
        System.out.printf("Multiverse > Performance %s transactions/second/thread\n",
                format(transactionsPerSecondPerThread));
        System.out.printf("Multiverse > Performance %s transactions/second\n",
                format(transactionsPerSecond));

        testCaseResult.put("transactionsPerSecondPerThread", transactionsPerSecondPerThread);
        testCaseResult.put("transactionsPerSecond", transactionsPerSecond);
    }

    class IncThread extends TestThread {
        private long durationMs;
        private final BetaLongRef ref;

        public IncThread(int id, BetaLongRef ref) {
            super("IncThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.ref = ref;
        }

        public void doRun() {
            long startMs = System.currentTimeMillis();
            final long _transactionsPerThread = transactionsPerThread;
            for (long k = 0; k < _transactionsPerThread; k++) {
                ref.atomicIncrementAndGet(1);
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse > %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}


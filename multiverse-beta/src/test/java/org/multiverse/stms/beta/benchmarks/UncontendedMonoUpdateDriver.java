package org.multiverse.stms.beta.benchmarks;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.api.LockLevel;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecond;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondPerThread;

public class UncontendedMonoUpdateDriver extends BenchmarkDriver {

    private BetaStm stm;
    private UpdateThread[] threads;

    private int threadCount = 1;
    private int transactionsPerThread = 100 * 1000 * 1000;
    private boolean dirtyCheck = false;
    private LockLevel lockLevel = LockLevel.LockNone;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Thread count %s \n", threadCount);
        System.out.printf("Multiverse > Transactions per thread %s \n", transactionsPerThread);
        System.out.printf("Multiverse > Dirtycheck %s \n", dirtyCheck);
        System.out.printf("Multiverse > Locklevel %s \n", lockLevel);

        stm = new BetaStm();

        threads = new UpdateThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k);
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
        for (UpdateThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);
        double transactionsPerSecond = transactionsPerSecond(transactionsPerThread, totalDurationMs, threadCount);
        System.out.printf("Multiverse > Performance %s transactions/second/thread\n",
                format(transactionsPerSecondPerThread));
        System.out.printf("Multiverse > Performance %s transactions/second\n",
                format(transactionsPerSecond));

        testCaseResult.put("transactionsPerSecondPerThread", transactionsPerSecondPerThread);
        testCaseResult.put("transactionsPerSecond", transactionsPerSecond);
    }

    class UpdateThread extends TestThread {
        private long durationMs;

        public UpdateThread(int id) {
            super("UpdateThread-" + id);
        }

        public void doRun() {
            final BetaLongRef ref = newLongRef(stm);

            LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setLockLevel(lockLevel)
                            .setDirtyCheckEnabled(dirtyCheck));
            long startMs = System.currentTimeMillis();
            final long _transactionCount = transactionsPerThread;
            for (long k = 0; k < _transactionCount; k++) {
                tx.openForWrite(ref, LOCKMODE_NONE).value++;
                tx.commit();
                tx.hardReset();
            }

            assertEquals(transactionsPerThread, ref.atomicGet());

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse > %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

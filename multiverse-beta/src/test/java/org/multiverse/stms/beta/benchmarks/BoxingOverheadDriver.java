package org.multiverse.stms.beta.benchmarks;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.api.LockLevel;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.BetaStmTestUtils.newRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

public class BoxingOverheadDriver extends BenchmarkDriver implements BetaStmConstants {

    private BetaStm stm;
    private boolean withBoxing;
    private long transactionsPerThread;
    private int threadCount;
    private UpdateThread[] threads;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Transactions/thread %s \n", format(transactionsPerThread));
        System.out.printf("Multiverse > ThreadCount %s \n", threadCount);
        System.out.printf("Multiverse > With Boxing %s \n", withBoxing);

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

        double transactionsPerSecond = transactionsPerSecond(
                transactionsPerThread, totalDurationMs,threadCount);

        System.out.printf("Multiverse > Performance %s transactions/second/thread\n",
                BenchmarkUtils.format(transactionsPerSecondPerThread));
        System.out.printf("Multiverse > Performance %s transactions/second\n",
                BenchmarkUtils.format(transactionsPerSecond));

        testCaseResult.put("transactionsPerThreadPerSecond", transactionsPerSecondPerThread);
        testCaseResult.put("transactionsPerSecond",transactionsPerSecond);
    }

    class UpdateThread extends TestThread {
        private long durationMs;

        public UpdateThread(int id) {
            super("UpdateThread-" + id);
        }

        public void doRun() {
            LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setLockLevel(LockLevel.CommitLockReads)
                            .setDirtyCheckEnabled(false));

            long startMs = System.currentTimeMillis();

            final long _transactionCount = transactionsPerThread;
            if (withBoxing) {
                BetaRef<Long> ref = newRef(stm, new Long(0));
                for (long k = 0; k < _transactionCount; k++) {
                    tx.openForWrite(ref, LOCKMODE_NONE).value++;
                    tx.commit();
                    tx.hardReset();
                }
                assertEquals(_transactionCount, (long) ref.atomicGet());
            } else {
                BetaLongRef ref = newLongRef(stm);
                for (long k = 0; k < _transactionCount; k++) {
                    tx.openForWrite(ref, LOCKMODE_NONE).value++;
                    tx.commit();
                    tx.hardReset();
                }
                assertEquals(_transactionCount, ref.atomicGet());
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse > %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

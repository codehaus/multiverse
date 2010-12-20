package org.multiverse.stms.beta.benchmarks;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.api.LockLevel;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecond;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondPerThread;

public class MonoReadDriver extends BenchmarkDriver implements BetaStmConstants{

    private BetaStm stm;
    private ReadThread[] threads;
    private int threadCount;
    private long transactionsPerThread;
    private int lockMode = LOCKMODE_NONE;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Thread count is %s\n", threadCount);
        System.out.printf("Multiverse > Transactions/thread is %s\n", transactionsPerThread);

        stm = new BetaStm();

        threads = new ReadThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k, transactionsPerThread);
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
        for (ReadThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);
        double transactionsPerSecond = transactionsPerSecond(transactionsPerThread,totalDurationMs,threadCount);
        System.out.printf("Multiverse > Performance %s transactions/second/thread\n",
                format(transactionsPerSecondPerThread));
        System.out.printf("Multiverse > Performance %s transactions/second\n",
                format(transactionsPerSecond));

        testCaseResult.put("transactionsPerSecond",transactionsPerSecond);
        testCaseResult.put("transactionsPerSecondPerThread", transactionsPerSecondPerThread);
    }

    class ReadThread extends TestThread {
        private final long transactionCount;
        private long durationMs;

        public ReadThread(int id, long transactionCount) {
            super("ReadThread-" + id);
            this.transactionCount = transactionCount;
        }

        public void doRun() {
            BetaLongRef ref = newLongRef(stm);

            LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setLockLevel(LockLevel.CommitLockReads)
                            .setDirtyCheckEnabled(false));
            long startMs = System.currentTimeMillis();
            final long _transactionCount = transactionCount;
            for (long k = 0; k < _transactionCount; k++) {
                tx.openForRead(ref, lockMode);
                tx.commit();
                tx.hardReset();
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse > %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}


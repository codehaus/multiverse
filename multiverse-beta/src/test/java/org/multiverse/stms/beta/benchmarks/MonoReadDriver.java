package org.multiverse.stms.beta.benchmarks;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondPerThread;

public class MonoReadDriver extends BenchmarkDriver implements BetaStmConstants{

    private BetaStm stm;
    private UpdateThread[] threads;
    private int threadCount;
    private long transactionsPerThread;
    private int lockMode = LOCKMODE_NONE;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Running with %s thread(s)\n", threadCount);
        System.out.printf("Multiverse > Running with %s transactionsPerThread\n", transactionsPerThread);

        stm = new BetaStm();

        threads = new UpdateThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, transactionsPerThread);
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
        System.out.printf("Multiverse > Performance %s transactions/second/thread\n",
                format(transactionsPerSecondPerThread));
        System.out.printf("Multiverse > Performance %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        testCaseResult.put("transactionsPerSecondPerThread", transactionsPerSecondPerThread);
    }

    class UpdateThread extends TestThread {
        private final long transactionCount;
        private long durationMs;

        public UpdateThread(int id, long transactionCount) {
            super("UpdateThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.transactionCount = transactionCount;
        }

        public void doRun() {
            BetaLongRef ref = newLongRef(stm);

            LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads)
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


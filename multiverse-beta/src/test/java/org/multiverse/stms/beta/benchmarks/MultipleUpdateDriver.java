package org.multiverse.stms.beta.benchmarks;

import org.benchy.AbstractBenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatArrayBetaTransaction;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmTestUtils.newReadBiasedLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;

/**
 * @author Peter Veentjer
 */
public class MultipleUpdateDriver extends AbstractBenchmarkDriver implements BetaStmConstants {

    private BetaStm stm;

    private long transactionsPerThread ;
    private int refCount;
    private int threadCount;
    private WriteThread[] threads;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Multiple update transaction benchmark\n");
        System.out.printf("Multiverse > Running with %s ref per transaction\n", refCount);
        System.out.printf("Multiverse > %s Transactions per thread\n", format(transactionsPerThread));

        stm = new BetaStm();
        threads = new WriteThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new WriteThread(k, refCount);
        }
    }

    @Override
    public void processResults(TestCaseResult testCaseResult) {
        long totalDurationMs = 0;
        for (WriteThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = BenchmarkUtils.transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);
        double transactionsPerSecond = BenchmarkUtils.transactionsPerSecond(
                transactionsPerThread, totalDurationMs, threadCount);
        System.out.printf("Multiverse > Performance %s transactions/second with %s threads\n",
                format(transactionsPerSecondPerThread), threadCount);
        System.out.printf("Multiverse > Performance %s transactions/second with %s threads\n",
                format(transactionsPerSecond), threadCount);

        testCaseResult.put("transactionsPerSecondPerThread", transactionsPerSecondPerThread);
        testCaseResult.put("transactionsPerSecond", transactionsPerSecond);
    }

    @Override
    public void run(TestCaseResult testCaseResult) {
        System.out.printf("Multiverse > Running with %s threads\n", threadCount);

        startAll(threads);
        joinAll(threads);
    }

    class WriteThread extends TestThread {
        private final int refCount;
        private long durationMs;

        public WriteThread(int id, int refCount) {
            super("WriteThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.refCount = refCount;
        }

        public void doRun() {
            BetaLongRef[] refs = new BetaLongRef[refCount];
            for (int k = 0; k < refCount; k++) {
                refs[k] = newReadBiasedLongRef(stm);
            }

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refs.length);

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();


            final long t = transactionsPerThread;
            for (int iteration = 0; iteration < t; iteration++) {
                for (int k = 0; k < refs.length; k++) {
                    tx.openForWrite(refs[0], LOCKMODE_NONE).value++;
                }
                tx.commit();
                tx.hardReset();

                if (iteration % 100000000 == 0 && iteration > 0) {
                    System.out.printf("Multiverse > %s is at %s\n", getName(), iteration);
                }
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse > %s is finished in %s ms\n", getName(), durationMs);
        }
    }

}

package org.multiverse.stms.beta.benchmarks;

import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

/**
 * @author Peter Veentjer
 */
public class UncontendedFatReadScalabilityTest {

    private BetaStm stm;

    public static void main(String[] args) {
        UncontendedFatReadScalabilityTest test = new UncontendedFatReadScalabilityTest();
        test.start(Long.parseLong(args[0]));
    }

    public void start(long transactionCount) {
        int[] processors = generateProcessorRange();

        System.out.println("Multiverse> Uncontended readonly fat-transaction benchmark");
        System.out.println("Multiverse> 1 BetaRef per transaction");
        System.out.printf("Multiverse> Running with the following processor range %s\n", Arrays.toString(processors));
        System.out.printf("Multiverse> %s Transactions per thread\n", format(transactionCount));
        Result[] result = new Result[processors.length];

        System.out.println("Multiverse> Starting warmup run");
        test(1, transactionCount);
        System.out.println("Multiverse> Finished warmup run");


        long startNs = System.nanoTime();

        for (int k = 0; k < processors.length; k++) {
            int processorCount = processors[k];
            double performance = test(processorCount, transactionCount);
            result[k] = new Result(processorCount, performance);
        }

        long durationNs = System.nanoTime() - startNs;
        System.out.printf("Benchmark took %s seconds\n", TimeUnit.NANOSECONDS.toSeconds(durationNs));

        toGnuplot(result);
    }

    private double test(int threadCount, long transactionsPerThread) {
        System.out.printf("Multiverse> ----------------------------------------------\n");
        System.out.printf("Multiverse> Running with %s thread(s)\n", threadCount);

        stm = new BetaStm();

        ReadThread[] threads = new ReadThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k, transactionsPerThread);
        }

        for (ReadThread thread : threads) {
            thread.start();
        }

        for (ReadThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        long totalDurationMs = 0;
        for (ReadThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = BenchmarkUtils.transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second/thread\n",
                format(transactionsPerSecondPerThread));
        System.out.printf("Multiverse> Performance %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));
        return transactionsPerSecondPerThread;
    }

    @SuppressWarnings({"UnusedAssignment"})
    class ReadThread extends Thread {
        private final long transactionCount;
        private long durationMs;

        public ReadThread(int id, long transactionCount) {
            super("ReadThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.transactionCount = transactionCount;
        }

        public void run() {
            BetaLongRef ref = BetaStmUtils.createReadBiasedLongRef(stm);

            FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfiguration(stm).setReadonly(true));
            long startMs = System.currentTimeMillis();

            //ArrayUpdateTransaction tx = new ArrayUpdateTransaction(stm, 1);
            for (long k = 0; k < transactionCount; k++) {
                //long x = ((LongRefTranlocal)(tx.openForRead((BetaTransactionalObject)ref, true, pool))).value;
                long x = tx.openForRead(ref, false).value;
                tx.commit();
                tx.hardReset();
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

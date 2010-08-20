package org.multiverse.benchmarks;

import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.LongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.multiverse.benchmarks.BenchmarkUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.format;

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
        loadOtherTransactionalObjectClasses();

        int[] processors = generateProcessorRange();

        System.out.println("Multiverse> Uncontended readonly fat-transaction benchmark");
        System.out.println("Multiverse> 1 Ref per transaction");
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

    private double test(int threadCount, long transactionCount) {
        System.out.printf("Multiverse> Running with %s processors\n", threadCount);

        stm = new BetaStm();

        ReadThread[] threads = new ReadThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k, transactionCount);
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

        double transactionsPerSecond = BenchmarkUtils.perSecond(
                transactionCount, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second with %s threads\n",
                format(transactionsPerSecond), threadCount);
        return transactionsPerSecond;
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
            LongRef ref = BetaStmUtils.createReadBiasedLongRef(stm);

            BetaObjectPool pool = new BetaObjectPool();

            FatMonoBetaTransaction tx = new FatMonoBetaTransaction(new BetaTransactionConfiguration(stm).setReadonly(true));
            long startMs = System.currentTimeMillis();

            //ArrayUpdateTransaction tx = new ArrayUpdateTransaction(stm, 1);
            for (long k = 0; k < transactionCount; k++) {
                //long x = ((LongRefTranlocal)(tx.openForRead((BetaTransactionalObject)ref, true, pool))).value;
                long x = tx.openForRead(ref, false, pool).value;
                tx.commit(pool);
                tx.hardReset(pool);
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

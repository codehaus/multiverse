package org.multiverse.benchmarks;

import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.transactions.ArrayTreeBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.benchmarks.BenchmarkUtils.*;
import static org.multiverse.stms.beta.StmUtils.format;

/**
 * @author Peter Veentjer
 */
public class UncontendedUpdateScalabilityTest {
    private BetaStm stm;

    public static void main(String[] args) {
        UncontendedUpdateScalabilityTest test = new UncontendedUpdateScalabilityTest();
        test.start(Long.parseLong(args[0]));
    }

    public void start(long transactionCount) {
        loadOtherTransactionalObjectClasses();

        int[] processors = generateProcessorRange();

        System.out.printf("Multiverse> Uncontended update transaction benchmark\n");
        System.out.printf("Multiverse> 1 Ref per transaction\n");
        System.out.printf("Multiverse> %s Transactions per thread\n", format(transactionCount));
        System.out.printf("Multiverse> Running with the following processor range %s\n", Arrays.toString(processors));
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
        System.out.printf("Multiverse> Benchmark took %s seconds\n", TimeUnit.NANOSECONDS.toSeconds(durationNs));

        toGnuplot(result);
    }

    private double test(int threadCount, long transactionCount) {
        System.out.printf("Multiverse> Running with %s processors\n", threadCount);

        stm = new BetaStm();

        UpdateThread[] threads = new UpdateThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, transactionCount);
        }

        long startNs = System.nanoTime();

        for (UpdateThread thread : threads) {
            thread.start();
        }

        for (UpdateThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        long totalDurationMs = 0;
        for (UpdateThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecond = BenchmarkUtils.perSecond(transactionCount, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second with %s threads\n", StmUtils.format(transactionsPerSecond), threadCount);
        return transactionsPerSecond;
    }

    class UpdateThread extends Thread {
        private final long transactionCount;
        private long durationMs;

        public UpdateThread(int id, long transactionCount) {
            super("UpdateThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.transactionCount = transactionCount;
        }

        public void run() {
            LongRef ref = StmUtils.createLongRef(stm);

            ObjectPool pool = new ObjectPool();

            //ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
            //ArrayBetaTransaction tx = new ArrayBetaTransaction(stm,1);
            MonoBetaTransaction tx = new MonoBetaTransaction(new BetaTransactionConfig(stm).setDirtyCheckEnabled(false));
            long startMs = System.currentTimeMillis();
            for (long k = 0; k < transactionCount; k++) {
                //((LongRefTranlocal)(tx.openForWrite((BetaTransactionalObject)ref, true, pool))).value++;

                tx.openForWrite(ref, true, pool).value++;
                tx.commit(pool);
                tx.reset(pool);

                //if (k % 100000000 == 0 && k > 0) {
                //    System.out.printf("%s is at %s\n", getName(), k);
                //}
            }

            assertEquals(transactionCount, ref.unsafeLoad().value);

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

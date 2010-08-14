package org.multiverse.benchmarks;

import org.multiverse.stms.beta.*;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.FatArrayBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.multiverse.benchmarks.BenchmarkUtils.generateProcessorRange;
import static org.multiverse.benchmarks.BenchmarkUtils.toGnuplot;
import static org.multiverse.stms.beta.BetaStmUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;

/**
 * @author Peter Veentjer
 */
public class MultipleWriteScalabilityTest {

    private BetaStm stm;
    private final long transactionCount = 100 * 1000 * 1000;


    public static void main(String[] args) {
        int refCount = Integer.parseInt(args[0]);
        MultipleWriteScalabilityTest test = new MultipleWriteScalabilityTest();

        test.start(refCount);
    }

    public void start(int refCount) {
        int[] processors = generateProcessorRange();

        System.out.printf("Multiverse> Unshared update transaction benchmark\n");
        System.out.printf("Multiverse> Running with the following processor range %s\n", Arrays.toString(processors));
        System.out.printf("Multiverse> Running with %s refs per transaction\n", refCount);
        System.out.printf("Multiverse> %s Transactions per thread\n", format(transactionCount));
        Result[] result = new Result[processors.length];

        System.out.printf("Multiverse> Starting warmup run\n");
        test(1, 1);
        System.out.printf("Multiverse> Finished warmup run\n");

        long startNs = System.nanoTime();

        for (int k = 0; k < processors.length; k++) {
            int processorCount = processors[k];
            double performance = test(processorCount, refCount);
            result[k] = new Result(processorCount, performance);
        }

        long durationNs = System.nanoTime() - startNs;
        System.out.printf("Benchmark took %s seconds\n", TimeUnit.NANOSECONDS.toSeconds(durationNs));

        toGnuplot(result);
    }


    private double test(int threadCount, int refCount) {
        System.out.printf("Multiverse> Running with %s processors\n", threadCount);

        stm = new BetaStm();

        ReadThread[] threads = new ReadThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k, refCount);
        }

        long startNs = System.nanoTime();

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

        double transactionsPerSecond = BenchmarkUtils.perSecond(transactionCount, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second with %s threads\n",
                format(transactionsPerSecond), threadCount);
        return transactionsPerSecond;
    }

    class ReadThread extends Thread {
        private final int refCount;
        private long durationMs;

        public ReadThread(int id, int refCount) {
            super("ReadThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.refCount = refCount;
        }

        public void run() {
            LongRef[] refs = new LongRef[refCount];
            for (int k = 0; k < refCount; k++) {
                refs[k] = createReadBiasedLongRef(stm);
            }

            BetaObjectPool pool = new BetaObjectPool();

            //AnotherInlinedMonoUpdateTransaction tx = new AnotherInlinedMonoUpdateTransaction(stm);
            //MonoUpdateTransaction tx = new MonoUpdateTransaction(stm);
            BetaTransactionConfig config = new BetaTransactionConfig(stm, refs.length);

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();


            for (int iteration = 0; iteration < transactionCount; iteration++) {
                for (int k = 0; k < refs.length; k++) {
                    LongRefTranlocal tranlocal = (LongRefTranlocal)tx.openForWrite(refs[0], false, pool);
                    tranlocal.value++;
                }
                tx.commit(pool);
                tx.hardReset(pool);

                //if (k % 100000000 == 0 && k > 0) {
                //    System.out.printf("%s is at %s\n", getName(), k);
                //}
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }

}

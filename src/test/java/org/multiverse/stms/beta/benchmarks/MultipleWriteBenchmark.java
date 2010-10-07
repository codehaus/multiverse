package org.multiverse.stms.beta.benchmarks;

import org.multiverse.TestThread;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatArrayBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmTestUtils.newReadBiasedLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

/**
 * @author Peter Veentjer
 */
public class MultipleWriteBenchmark implements BetaStmConstants {

    private BetaStm stm;
    private final long transactionsPerThread = 100 * 1000 * 1000;


    public static void main(String[] args) {
        int refCount = Integer.parseInt(args[0]);
        MultipleWriteBenchmark test = new MultipleWriteBenchmark();

        test.start(refCount);
    }

    public void start(int refCount) {
        int[] processors = generateProcessorRange();

        System.out.printf("Multiverse> Unshared update transaction benchmark\n");
        System.out.printf("Multiverse> Running with the following processor range %s\n", Arrays.toString(processors));
        System.out.printf("Multiverse> Running with %s transactionalobjects per transaction\n", refCount);
        System.out.printf("Multiverse> %s Transactions per thread\n", format(transactionsPerThread));
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
        System.out.printf("Multiverse> ----------------------------------------------\n");
        System.out.printf("Multiverse> Running with %s processors\n", threadCount);

        stm = new BetaStm();

        ReadThread[] threads = new ReadThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k, refCount);
        }


        startAll(threads);
        joinAll(threads);

        long totalDurationMs = 0;
        for (ReadThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = BenchmarkUtils.transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second with %s threads\n",
                format(transactionsPerSecondPerThread), threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        return transactionsPerSecondPerThread;
    }

    class ReadThread extends TestThread {
        private final int refCount;
        private long durationMs;

        public ReadThread(int id, int refCount) {
            super("ReadThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.refCount = refCount;
        }

        public void doRun() {
            BetaLongRef[] refs = new BetaLongRef[refCount];
            for (int k = 0; k < refCount; k++) {
                refs[k] = newReadBiasedLongRef(stm);
            }

            //AnotherInlinedMonoUpdateTransaction tx = new AnotherInlinedMonoUpdateTransaction(stm);
            //MonoUpdateTransaction tx = new MonoUpdateTransaction(stm);
            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, refs.length);

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();


            for (int iteration = 0; iteration < transactionsPerThread; iteration++) {
                for (int k = 0; k < refs.length; k++) {
                    tx.openForWrite(refs[0], LOCKMODE_NONE).value++;
                }
                tx.commit();
                tx.hardReset();

                //if (k % 100000000 == 0 && k > 0) {
                //    System.out.printf("%s is at %s\n", getName(), k);
                //}
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }

}

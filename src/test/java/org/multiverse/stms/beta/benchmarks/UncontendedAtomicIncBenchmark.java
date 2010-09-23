package org.multiverse.stms.beta.benchmarks;

import org.multiverse.TestThread;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

public class UncontendedAtomicIncBenchmark {

    private BetaStm stm;

    public static void main(String[] args) {
        UncontendedAtomicIncBenchmark test = new UncontendedAtomicIncBenchmark();
        test.start(Long.parseLong(args[0]));
    }

    public void start(long transactionCount) {
        int[] processors = generateProcessorRange();

        System.out.printf("Multiverse> Uncontended atomicInc transaction benchmark\n");
        System.out.printf("Multiverse> %s Transactions per thread\n", format(transactionCount));
        System.out.printf("Multiverse> Running with the following processor range %s\n", Arrays.toString(processors));
        Result[] result = new Result[processors.length];

        System.out.printf("Multiverse> Starting warmup run\n");
        test(1, transactionCount);
        System.out.printf("Multiverse> Finished warmup run\n");

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

    private double test(int threadCount, long transactionsPerThread) {
        System.out.printf("Multiverse> ----------------------------------------------\n");
        System.out.printf("Multiverse> Running with %s thread(s)\n", threadCount);

        stm = new BetaStm();

        UpdateThread[] threads = new UpdateThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k, transactionsPerThread);
        }

        startAll(threads);
        joinAll(threads);

        long totalDurationMs = 0;
        for (UpdateThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second/thread with %s threads\n",
                format(transactionsPerSecondPerThread), threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        return transactionsPerSecondPerThread;
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
            BetaLongRef ref = newLongRef(stm, -1);

            long startMs = System.currentTimeMillis();
            for (long k = 0; k < transactionCount; k++) {
                ref.atomicIncrementAndGet(1);
            }

            assertEquals(transactionCount, ref.atomicGet() + 1);

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

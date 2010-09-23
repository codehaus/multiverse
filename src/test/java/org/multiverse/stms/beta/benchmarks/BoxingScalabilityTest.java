package org.multiverse.stms.beta.benchmarks;

import org.multiverse.TestThread;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.BetaStmUtils.*;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

public class BoxingScalabilityTest {

    private BetaStm stm;

    public static void main(String[] args) {
        BoxingScalabilityTest test = new BoxingScalabilityTest();
        test.start(Long.parseLong(args[0]));
    }

    public void start(long transactionsPerThread) {
        int[] processors = generateProcessorRange();

        System.out.printf("Multiverse> Boxing scalability benchmark\n");
        System.out.printf("Multiverse> 1 BetaRef per transaction\n");
        System.out.printf("Multiverse> %s Transactions per thread\n", format(transactionsPerThread));
        System.out.printf("Multiverse> Running with the following processor range %s\n", Arrays.toString(processors));

        System.out.printf("========================================================\n");
        System.out.println("Multiverse> Starting warmup run");
        testWithPrimitives(1, transactionsPerThread);
        testWithBoxing(1, transactionsPerThread);
        System.out.printf("========================================================\n");
        System.out.println("Multiverse> Finished warmup run");

        long startNs = System.nanoTime();

        for (int k = 0; k < processors.length; k++) {
            int processorCount = processors[k];
            double primitivePerformance = testWithPrimitives(processorCount, transactionsPerThread);
            double boxingPerformance = testWithBoxing(processorCount, transactionsPerThread);

            System.out.printf("========================================================\n");
            System.out.printf("Multiverse> With %s threads the primitive/boxing performance = %s\n",
                    (k + 1), primitivePerformance / boxingPerformance);
        }

        long durationNs = System.nanoTime() - startNs;
        System.out.printf("Multiverse> Benchmark took %s seconds\n", TimeUnit.NANOSECONDS.toSeconds(durationNs));
    }

    private double testWithPrimitives(int threadCount, long transactionsPerThread) {
        System.out.printf("Multiverse> ----------------------------------------------\n");
        System.out.printf("Multiverse> Primitive running with %s thread(s)\n", threadCount);

        stm = new BetaStm();

        PrimitiveThread[] threads = new PrimitiveThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PrimitiveThread(k, transactionsPerThread);
        }

        startAll(threads);
        joinAll(threads);

        long totalDurationMs = 0;
        for (PrimitiveThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);

        System.out.printf("Multiverse> Threadcount %s\n", threadCount);
        System.out.printf("Multiverse> Primitive performance %s transactions/second/thread\n",
                BenchmarkUtils.format(transactionsPerSecondPerThread));
        System.out.printf("Multiverse> Primitive Performance %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        return transactionsPerSecondPerThread;
    }

    private double testWithBoxing(int threadCount, long transactionsPerThread) {
        System.out.printf("Multiverse> ----------------------------------------------\n");
        System.out.printf("Multiverse> Boxing running with %s thread(s)\n", threadCount);

        stm = new BetaStm();

        BoxingThread[] threads = new BoxingThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new BoxingThread(k, transactionsPerThread);
        }

        startAll(threads);
        joinAll(threads);

        long totalDurationMs = 0;
        for (BoxingThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecondPerThread = transactionsPerSecondPerThread(
                transactionsPerThread, totalDurationMs, threadCount);

        System.out.printf("Multiverse> Threadcount %s\n", threadCount);
        System.out.printf("Multiverse> Boxing performance %s transactions/second/thread\n",
                BenchmarkUtils.format(transactionsPerSecondPerThread));
        System.out.printf("Multiverse> Boxing Performance %s transactions/second\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount));

        return transactionsPerSecondPerThread;
    }

    class PrimitiveThread extends TestThread {
        private final long transactionCount;
        private long durationMs;

        public PrimitiveThread(int id, long transactionCount) {
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
            for (long k = 0; k < transactionCount; k++) {
                tx.openForWrite(ref, true).value++;
                tx.commit();
                tx.hardReset();
            }

            assertEquals(transactionCount, ref.atomicGet());

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }

    class BoxingThread extends TestThread {
        private final long transactionCount;
        private long durationMs;

        public BoxingThread(int id, long transactionCount) {
            super("UpdateThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.transactionCount = transactionCount;
        }

        public void doRun() {
            BetaRef<Long> ref = newRef(stm, new Long(0));

            LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads)
                            .setDirtyCheckEnabled(false));
            long startMs = System.currentTimeMillis();
            for (long k = 0; k < transactionCount; k++) {
                tx.openForWrite(ref, true).value++;
                tx.commit();
                tx.hardReset();
            }

            assertEquals(transactionCount, (long) ref.atomicGet());

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

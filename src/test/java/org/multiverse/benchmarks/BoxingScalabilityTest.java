package org.multiverse.benchmarks;

import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.benchmarks.BenchmarkUtils.generateProcessorRange;
import static org.multiverse.stms.beta.BetaStmUtils.createRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;

public class BoxingScalabilityTest {

    private BetaStm stm;

    public static void main(String[] args) {
        BoxingScalabilityTest test = new BoxingScalabilityTest();
        test.start(Long.parseLong(args[0]));
    }

    public void start(long transactionCount) {
        int[] processors = generateProcessorRange();

        System.out.printf("Multiverse> Boxing scalability benchmark\n");
        System.out.printf("Multiverse> 1 BetaRef per transaction\n");
        System.out.printf("Multiverse> %s Transactions per thread\n", format(transactionCount));
        System.out.printf("Multiverse> Running with the following processor range %s\n", Arrays.toString(processors));

        System.out.println("Multiverse> Starting warmup run");
        testWithPrimitives(1, transactionCount);
        testWithBoxing(1, transactionCount);
        System.out.println("Multiverse> Finished warmup run");

        long startNs = System.nanoTime();

        for (int k = 0; k < processors.length; k++) {
            int processorCount = processors[k];
            double primitivePerformance = testWithPrimitives(processorCount, transactionCount);
            double boxingPerformance = testWithBoxing(processorCount, transactionCount);

            System.out.println(k+" Primitive times faster = "+primitivePerformance/boxingPerformance);
        }

        long durationNs = System.nanoTime() - startNs;
        System.out.printf("Multiverse> Benchmark took %s seconds\n", TimeUnit.NANOSECONDS.toSeconds(durationNs));
    }

    private double testWithPrimitives(int threadCount, long transactionCount) {
        System.out.printf("Multiverse> Primitive running with %s processors\n", threadCount);

        stm = new BetaStm();

        PrimitiveThread[] threads = new PrimitiveThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new PrimitiveThread(k, transactionCount);
        }

        for (PrimitiveThread thread : threads) {
            thread.start();
        }

        for (PrimitiveThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        long totalDurationMs = 0;
        for (PrimitiveThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecond = BenchmarkUtils.perSecond(transactionCount, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second with %s thread(s)\n", BetaStmUtils.format(transactionsPerSecond), threadCount);
        return transactionsPerSecond;
    }

    private double testWithBoxing(int threadCount, long transactionCount) {
        System.out.printf("Multiverse> Boxing running with %s processors\n", threadCount);

        stm = new BetaStm();

        BoxingThread[] threads = new BoxingThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new BoxingThread(k, transactionCount);
        }

        for (BoxingThread thread : threads) {
            thread.start();
        }

        for (BoxingThread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

        long totalDurationMs = 0;
        for (BoxingThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double transactionsPerSecond = BenchmarkUtils.perSecond(transactionCount, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second with %s thread(s)\n", BetaStmUtils.format(transactionsPerSecond), threadCount);
        return transactionsPerSecond;
    }

    class PrimitiveThread extends Thread {
        private final long transactionCount;
        private long durationMs;

        public PrimitiveThread(int id, long transactionCount) {
            super("UpdateThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.transactionCount = transactionCount;
        }

        public void run() {
            BetaLongRef ref = BetaStmUtils.createLongRef(stm);

            LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setPessimisticLockLevel(PessimisticLockLevel.Read)
                            .setDirtyCheckEnabled(false));
            long startMs = System.currentTimeMillis();
            for (long k = 0; k < transactionCount; k++) {
                tx.openForWrite(ref, true).value++;
                tx.commit();
                tx.hardReset();
            }

            assertEquals(transactionCount, ref.___unsafeLoad().value);

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }

      class BoxingThread extends Thread {
        private final long transactionCount;
        private long durationMs;

        public BoxingThread(int id, long transactionCount) {
            super("UpdateThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.transactionCount = transactionCount;
        }

        public void run() {
            BetaRef<Long> ref = createRef(stm, new Long(0));

            LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(
                    new BetaTransactionConfiguration(stm)
                            .setPessimisticLockLevel(PessimisticLockLevel.Read)
                            .setDirtyCheckEnabled(false));
            long startMs = System.currentTimeMillis();
            for (long k = 0; k < transactionCount; k++) {
                tx.openForWrite(ref, true).value++;
                tx.commit();
                tx.hardReset();
            }

            assertEquals(transactionCount, (long)ref.___unsafeLoad().value);

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

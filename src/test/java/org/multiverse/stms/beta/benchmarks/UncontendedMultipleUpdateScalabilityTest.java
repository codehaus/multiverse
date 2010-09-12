package org.multiverse.stms.beta.benchmarks;

import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatArrayBetaTransaction;
import org.multiverse.stms.beta.transactions.FatArrayTreeBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.BetaStmUtils.newReadBiasedLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.toGnuplot;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;

public class UncontendedMultipleUpdateScalabilityTest {

    private BetaStm stm;
    private final long transactionsPerThread = 100 * 1000 * 1000;


    public static void main(String[] args) {
        //should be a power of two.
        int refCount = Integer.parseInt(args[0]);
        UncontendedMultipleUpdateScalabilityTest test = new UncontendedMultipleUpdateScalabilityTest();

        test.start(refCount);
    }

    public void start(int refCount) {
        int[] processors = new int[]{1};

        System.out.printf("Multiverse> Uncontended multiple write transaction benchmark\n");
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
        System.out.printf("Multiverse> Running with %s processors\n", threadCount);

        stm = new BetaStm();

        ReadThread[] threads = new ReadThread[threadCount];

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k, refCount);
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

        double readsPerSecondPerThread = BenchmarkUtils.transactionsPerSecondPerThread(
                transactionsPerThread * refCount, totalDurationMs, threadCount);

        System.out.printf("Multiverse> Performance %s writes/second/thread with %s threads\n",
                format(readsPerSecondPerThread), threadCount);
        System.out.printf("Multiverse> Performance %s writes/second with %s threads\n",
                transactionsPerSecondAsString(transactionsPerThread * refCount, totalDurationMs, threadCount),
                threadCount);

        return readsPerSecondPerThread;
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
            switch (refCount) {
                case 1:
                    run1();
                    break;
                case 2:
                    run2();
                    break;
                case 4:
                    run4();
                    break;
                case 8:
                    run8();
                    break;
                case 16:
                    run16();
                    break;
                case 32:
                    run32();
                    break;
                case 64:
                    run64();
                    break;
                default:
                    throw new IllegalStateException();
            }
        }

        public void run1() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 1).init();;

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, false);
                tx.commit();
                tx.hardReset();
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run2() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);
            BetaLongRef ref2 = newReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 2).init();

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, false);
                tx.openForWrite(ref2, false);
                tx.commit();
                tx.hardReset();

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run4() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);
            BetaLongRef ref2 = newReadBiasedLongRef(stm);
            BetaLongRef ref3 = newReadBiasedLongRef(stm);
            BetaLongRef ref4 = newReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm,4).init();
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, false);
                tx.openForWrite(ref2, false);
                tx.openForWrite(ref3, false);
                tx.openForWrite(ref4, false);
                tx.commit();
                tx.hardReset();
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run8() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);
            BetaLongRef ref2 = newReadBiasedLongRef(stm);
            BetaLongRef ref3 = newReadBiasedLongRef(stm);
            BetaLongRef ref4 = newReadBiasedLongRef(stm);
            BetaLongRef ref5 = newReadBiasedLongRef(stm);
            BetaLongRef ref6 = newReadBiasedLongRef(stm);
            BetaLongRef ref7 = newReadBiasedLongRef(stm);
            BetaLongRef ref8 = newReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm,8).init();
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, false);
                tx.openForWrite(ref2, false);
                tx.openForWrite(ref3, false);
                tx.openForWrite(ref4, false);
                tx.openForWrite(ref5, false);
                tx.openForWrite(ref6, false);
                tx.openForWrite(ref7, false);
                tx.openForWrite(ref8, false);

                tx.commit();
                tx.hardReset();

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run16() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);
            BetaLongRef ref2 = newReadBiasedLongRef(stm);
            BetaLongRef ref3 = newReadBiasedLongRef(stm);
            BetaLongRef ref4 = newReadBiasedLongRef(stm);
            BetaLongRef ref5 = newReadBiasedLongRef(stm);
            BetaLongRef ref6 = newReadBiasedLongRef(stm);
            BetaLongRef ref7 = newReadBiasedLongRef(stm);
            BetaLongRef ref8 = newReadBiasedLongRef(stm);
            BetaLongRef ref9 = newReadBiasedLongRef(stm);
            BetaLongRef ref10 = newReadBiasedLongRef(stm);
            BetaLongRef ref11 = newReadBiasedLongRef(stm);
            BetaLongRef ref12 = newReadBiasedLongRef(stm);
            BetaLongRef ref13 = newReadBiasedLongRef(stm);
            BetaLongRef ref14 = newReadBiasedLongRef(stm);
            BetaLongRef ref15 = newReadBiasedLongRef(stm);
            BetaLongRef ref16 = newReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm,16);
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, false);
                tx.openForWrite(ref2, false);
                tx.openForWrite(ref3, false);
                tx.openForWrite(ref4, false);
                tx.openForWrite(ref5, false);
                tx.openForWrite(ref6, false);
                tx.openForWrite(ref7, false);
                tx.openForWrite(ref8, false);
                tx.openForWrite(ref9, false);
                tx.openForWrite(ref10, false);
                tx.openForWrite(ref11, false);
                tx.openForWrite(ref12, false);
                tx.openForWrite(ref13, false);
                tx.openForWrite(ref14, false);
                tx.openForWrite(ref15, false);
                tx.openForWrite(ref16, false);

                tx.commit();
                tx.hardReset();

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run32() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);
            BetaLongRef ref2 = newReadBiasedLongRef(stm);
            BetaLongRef ref3 = newReadBiasedLongRef(stm);
            BetaLongRef ref4 = newReadBiasedLongRef(stm);
            BetaLongRef ref5 = newReadBiasedLongRef(stm);
            BetaLongRef ref6 = newReadBiasedLongRef(stm);
            BetaLongRef ref7 = newReadBiasedLongRef(stm);
            BetaLongRef ref8 = newReadBiasedLongRef(stm);
            BetaLongRef ref9 = newReadBiasedLongRef(stm);
            BetaLongRef ref10 = newReadBiasedLongRef(stm);
            BetaLongRef ref11 = newReadBiasedLongRef(stm);
            BetaLongRef ref12 = newReadBiasedLongRef(stm);
            BetaLongRef ref13 = newReadBiasedLongRef(stm);
            BetaLongRef ref14 = newReadBiasedLongRef(stm);
            BetaLongRef ref15 = newReadBiasedLongRef(stm);
            BetaLongRef ref16 = newReadBiasedLongRef(stm);
            BetaLongRef ref17 = newReadBiasedLongRef(stm);
            BetaLongRef ref18 = newReadBiasedLongRef(stm);
            BetaLongRef ref19 = newReadBiasedLongRef(stm);
            BetaLongRef ref20 = newReadBiasedLongRef(stm);
            BetaLongRef ref21 = newReadBiasedLongRef(stm);
            BetaLongRef ref22 = newReadBiasedLongRef(stm);
            BetaLongRef ref23 = newReadBiasedLongRef(stm);
            BetaLongRef ref24 = newReadBiasedLongRef(stm);
            BetaLongRef ref25 = newReadBiasedLongRef(stm);
            BetaLongRef ref26 = newReadBiasedLongRef(stm);
            BetaLongRef ref27 = newReadBiasedLongRef(stm);
            BetaLongRef ref28 = newReadBiasedLongRef(stm);
            BetaLongRef ref29 = newReadBiasedLongRef(stm);
            BetaLongRef ref30 = newReadBiasedLongRef(stm);
            BetaLongRef ref31 = newReadBiasedLongRef(stm);
            BetaLongRef ref32 = newReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm,32).init();
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, false);
                tx.openForWrite(ref2, false);
                tx.openForWrite(ref3, false);
                tx.openForWrite(ref4, false);
                tx.openForWrite(ref5, false);
                tx.openForWrite(ref6, false);
                tx.openForWrite(ref7, false);
                tx.openForWrite(ref8, false);
                tx.openForWrite(ref9, false);
                tx.openForWrite(ref10, false);
                tx.openForWrite(ref11, false);
                tx.openForWrite(ref12, false);
                tx.openForWrite(ref13, false);
                tx.openForWrite(ref14, false);
                tx.openForWrite(ref15, false);
                tx.openForWrite(ref16, false);
                tx.openForWrite(ref17, false);
                tx.openForWrite(ref18, false);
                tx.openForWrite(ref19, false);
                tx.openForWrite(ref20, false);
                tx.openForWrite(ref21, false);
                tx.openForWrite(ref22, false);
                tx.openForWrite(ref23, false);
                tx.openForWrite(ref24, false);
                tx.openForWrite(ref25, false);
                tx.openForWrite(ref26, false);
                tx.openForWrite(ref27, false);
                tx.openForWrite(ref28, false);
                tx.openForWrite(ref29, false);
                tx.openForWrite(ref30, false);
                tx.openForWrite(ref31, false);
                tx.openForWrite(ref32, false);

                tx.commit();
                tx.hardReset();

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run64() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);
            BetaLongRef ref2 = newReadBiasedLongRef(stm);
            BetaLongRef ref3 = newReadBiasedLongRef(stm);
            BetaLongRef ref4 = newReadBiasedLongRef(stm);
            BetaLongRef ref5 = newReadBiasedLongRef(stm);
            BetaLongRef ref6 = newReadBiasedLongRef(stm);
            BetaLongRef ref7 = newReadBiasedLongRef(stm);
            BetaLongRef ref8 = newReadBiasedLongRef(stm);
            BetaLongRef ref9 = newReadBiasedLongRef(stm);
            BetaLongRef ref10 = newReadBiasedLongRef(stm);
            BetaLongRef ref11 = newReadBiasedLongRef(stm);
            BetaLongRef ref12 = newReadBiasedLongRef(stm);
            BetaLongRef ref13 = newReadBiasedLongRef(stm);
            BetaLongRef ref14 = newReadBiasedLongRef(stm);
            BetaLongRef ref15 = newReadBiasedLongRef(stm);
            BetaLongRef ref16 = newReadBiasedLongRef(stm);
            BetaLongRef ref17 = newReadBiasedLongRef(stm);
            BetaLongRef ref18 = newReadBiasedLongRef(stm);
            BetaLongRef ref19 = newReadBiasedLongRef(stm);
            BetaLongRef ref20 = newReadBiasedLongRef(stm);
            BetaLongRef ref21 = newReadBiasedLongRef(stm);
            BetaLongRef ref22 = newReadBiasedLongRef(stm);
            BetaLongRef ref23 = newReadBiasedLongRef(stm);
            BetaLongRef ref24 = newReadBiasedLongRef(stm);
            BetaLongRef ref25 = newReadBiasedLongRef(stm);
            BetaLongRef ref26 = newReadBiasedLongRef(stm);
            BetaLongRef ref27 = newReadBiasedLongRef(stm);
            BetaLongRef ref28 = newReadBiasedLongRef(stm);
            BetaLongRef ref29 = newReadBiasedLongRef(stm);
            BetaLongRef ref30 = newReadBiasedLongRef(stm);
            BetaLongRef ref31 = newReadBiasedLongRef(stm);
            BetaLongRef ref32 = newReadBiasedLongRef(stm);
            BetaLongRef ref33 = newReadBiasedLongRef(stm);
            BetaLongRef ref34 = newReadBiasedLongRef(stm);
            BetaLongRef ref35 = newReadBiasedLongRef(stm);
            BetaLongRef ref36 = newReadBiasedLongRef(stm);
            BetaLongRef ref37 = newReadBiasedLongRef(stm);
            BetaLongRef ref38 = newReadBiasedLongRef(stm);
            BetaLongRef ref39 = newReadBiasedLongRef(stm);
            BetaLongRef ref40 = newReadBiasedLongRef(stm);
            BetaLongRef ref41 = newReadBiasedLongRef(stm);
            BetaLongRef ref42 = newReadBiasedLongRef(stm);
            BetaLongRef ref43 = newReadBiasedLongRef(stm);
            BetaLongRef ref44 = newReadBiasedLongRef(stm);
            BetaLongRef ref45 = newReadBiasedLongRef(stm);
            BetaLongRef ref46 = newReadBiasedLongRef(stm);
            BetaLongRef ref47 = newReadBiasedLongRef(stm);
            BetaLongRef ref48 = newReadBiasedLongRef(stm);
            BetaLongRef ref49 = newReadBiasedLongRef(stm);
            BetaLongRef ref50 = newReadBiasedLongRef(stm);
            BetaLongRef ref51 = newReadBiasedLongRef(stm);
            BetaLongRef ref52 = newReadBiasedLongRef(stm);
            BetaLongRef ref53 = newReadBiasedLongRef(stm);
            BetaLongRef ref54 = newReadBiasedLongRef(stm);
            BetaLongRef ref55 = newReadBiasedLongRef(stm);
            BetaLongRef ref56 = newReadBiasedLongRef(stm);
            BetaLongRef ref57 = newReadBiasedLongRef(stm);
            BetaLongRef ref58 = newReadBiasedLongRef(stm);
            BetaLongRef ref59 = newReadBiasedLongRef(stm);
            BetaLongRef ref60 = newReadBiasedLongRef(stm);
            BetaLongRef ref61 = newReadBiasedLongRef(stm);
            BetaLongRef ref62 = newReadBiasedLongRef(stm);
            BetaLongRef ref63 = newReadBiasedLongRef(stm);
            BetaLongRef ref64 = newReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm,64).init();
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, false);
                tx.openForWrite(ref2, false);
                tx.openForWrite(ref3, false);
                tx.openForWrite(ref4, false);
                tx.openForWrite(ref5, false);
                tx.openForWrite(ref6, false);
                tx.openForWrite(ref7, false);
                tx.openForWrite(ref8, false);
                tx.openForWrite(ref9, false);
                tx.openForWrite(ref10, false);
                tx.openForWrite(ref11, false);
                tx.openForWrite(ref12, false);
                tx.openForWrite(ref13, false);
                tx.openForWrite(ref14, false);
                tx.openForWrite(ref15, false);
                tx.openForWrite(ref16, false);
                tx.openForWrite(ref17, false);
                tx.openForWrite(ref18, false);
                tx.openForWrite(ref19, false);
                tx.openForWrite(ref20, false);
                tx.openForWrite(ref21, false);
                tx.openForWrite(ref22, false);
                tx.openForWrite(ref23, false);
                tx.openForWrite(ref24, false);
                tx.openForWrite(ref25, false);
                tx.openForWrite(ref26, false);
                tx.openForWrite(ref27, false);
                tx.openForWrite(ref28, false);
                tx.openForWrite(ref29, false);
                tx.openForWrite(ref30, false);
                tx.openForWrite(ref31, false);
                tx.openForWrite(ref32, false);
                tx.openForWrite(ref33, false);
                tx.openForWrite(ref34, false);
                tx.openForWrite(ref35, false);
                tx.openForWrite(ref36, false);
                tx.openForWrite(ref37, false);
                tx.openForWrite(ref38, false);
                tx.openForWrite(ref39, false);
                tx.openForWrite(ref40, false);
                tx.openForWrite(ref41, false);
                tx.openForWrite(ref42, false);
                tx.openForWrite(ref43, false);
                tx.openForWrite(ref44, false);
                tx.openForWrite(ref45, false);
                tx.openForWrite(ref46, false);
                tx.openForWrite(ref47, false);
                tx.openForWrite(ref48, false);
                tx.openForWrite(ref49, false);
                tx.openForWrite(ref50, false);
                tx.openForWrite(ref51, false);
                tx.openForWrite(ref52, false);
                tx.openForWrite(ref53, false);
                tx.openForWrite(ref54, false);
                tx.openForWrite(ref55, false);
                tx.openForWrite(ref56, false);
                tx.openForWrite(ref57, false);
                tx.openForWrite(ref58, false);
                tx.openForWrite(ref59, false);
                tx.openForWrite(ref60, false);
                tx.openForWrite(ref61, false);
                tx.openForWrite(ref62, false);
                tx.openForWrite(ref63, false);
                tx.openForWrite(ref64, false);

                tx.commit();
                tx.hardReset();

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }

}

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
import static org.multiverse.stms.beta.BetaStmUtils.format;
import static org.multiverse.stms.beta.BetaStmUtils.newReadBiasedLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

public class UncontendedMultipleUpdateBenchmark implements BetaStmConstants {

    private BetaStm stm;
    private final long transactionsPerThread = 400 * 1000 * 1000;


    public static void main(String[] args) {
        //should be a power of two.
        int refCount = Integer.parseInt(args[0]);
        UncontendedMultipleUpdateBenchmark test = new UncontendedMultipleUpdateBenchmark();

        test.start(refCount);
    }

    public void start(int refCount) {
        int[] processors = new int[]{1};

        System.out.printf("Multiverse> Uncontended multiple write transaction benchmark\n");
        System.out.printf("Multiverse> Running with the following processor range %s\n", Arrays.toString(processors));
        System.out.printf("Multiverse> Running with %s transactionalobjects per transaction\n", refCount);
        System.out.printf("Multiverse> %s Transactions per thread\n", format(transactionsPerThread));
        Result[] result = new Result[processors.length];

        System.out.printf("Multiverse> Starting warmup run (1 ref per transaction and 1 thread)\n");
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

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        //.setDirtyCheckEnabled(true)
        //.setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);
        config.maxArrayTransactionSize = refCount;

        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k, refCount, config);
        }

        startAll(threads);
        joinAll(threads);

        long totalDurationMs = 0;
        for (ReadThread t : threads) {
            totalDurationMs += t.durationMs;
        }

        double readsPerSecondPerThread = BenchmarkUtils.transactionsPerSecondPerThread(
                transactionsPerThread * refCount, totalDurationMs, threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second with %s threads\n",
                transactionsPerSecondAsString(transactionsPerThread, totalDurationMs, threadCount), threadCount);
        System.out.printf("Multiverse> Performance %s transactions/second/thread with %s threads\n",
                transactionsPerSecondPerThreadAsString(transactionsPerThread, totalDurationMs, threadCount), threadCount);
        System.out.printf("Multiverse> Performance %s writes/second/thread with %s threads\n",
                format(readsPerSecondPerThread), threadCount);
        System.out.printf("Multiverse> Performance %s writes/second with %s threads\n",
                transactionsPerSecondAsString(transactionsPerThread * refCount, totalDurationMs, threadCount),
                threadCount);

        return readsPerSecondPerThread;
    }

    class ReadThread extends TestThread {
        private final int refCount;
        private long durationMs;
        private BetaTransactionConfiguration betaTransactionConfiguration;

        public ReadThread(int id, int refCount, BetaTransactionConfiguration betaTransactionConfiguration) {
            super("ReadThread-" + id);
            setPriority(Thread.MAX_PRIORITY);
            this.refCount = refCount;
            this.betaTransactionConfiguration = betaTransactionConfiguration;
        }

        public void doRun() {
            switch (refCount) {
                case 1:
                    run1();
                    break;
                case 2:
                    run2();
                    break;
                case 3:
                    run3();
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
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(betaTransactionConfiguration);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, LOCKMODE_NONE);
                tx.commit();
                tx.hardReset();
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run2() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);
            BetaLongRef ref2 = newReadBiasedLongRef(stm);

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(betaTransactionConfiguration);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, LOCKMODE_NONE).value++;
                tx.openForWrite(ref2, LOCKMODE_NONE).value++;
                tx.commit();
                tx.hardReset();

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run3() {
            BetaLongRef ref1 = newReadBiasedLongRef(stm);
            BetaLongRef ref2 = newReadBiasedLongRef(stm);
            BetaLongRef ref3 = newReadBiasedLongRef(stm);

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(betaTransactionConfiguration);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, LOCKMODE_NONE).value++;
                tx.openForWrite(ref2, LOCKMODE_NONE).value++;
                tx.openForWrite(ref3, LOCKMODE_NONE).value++;
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

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(betaTransactionConfiguration);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, LOCKMODE_NONE).value++;
                tx.openForWrite(ref2, LOCKMODE_NONE).value++;
                tx.openForWrite(ref3, LOCKMODE_NONE).value++;
                tx.openForWrite(ref4, LOCKMODE_NONE).value++;
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

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(betaTransactionConfiguration);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, LOCKMODE_NONE).value++;
                tx.openForWrite(ref2, LOCKMODE_NONE).value++;
                tx.openForWrite(ref3, LOCKMODE_NONE).value++;
                tx.openForWrite(ref4, LOCKMODE_NONE).value++;
                tx.openForWrite(ref5, LOCKMODE_NONE).value++;
                tx.openForWrite(ref6, LOCKMODE_NONE).value++;
                tx.openForWrite(ref7, LOCKMODE_NONE).value++;
                tx.openForWrite(ref8, LOCKMODE_NONE).value++;

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

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(betaTransactionConfiguration);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, LOCKMODE_NONE).value++;
                tx.openForWrite(ref2, LOCKMODE_NONE).value++;
                tx.openForWrite(ref3, LOCKMODE_NONE).value++;
                tx.openForWrite(ref4, LOCKMODE_NONE).value++;
                tx.openForWrite(ref5, LOCKMODE_NONE).value++;
                tx.openForWrite(ref6, LOCKMODE_NONE).value++;
                tx.openForWrite(ref7, LOCKMODE_NONE).value++;
                tx.openForWrite(ref8, LOCKMODE_NONE).value++;
                tx.openForWrite(ref9, LOCKMODE_NONE).value++;
                tx.openForWrite(ref10, LOCKMODE_NONE).value++;
                tx.openForWrite(ref11, LOCKMODE_NONE).value++;
                tx.openForWrite(ref12, LOCKMODE_NONE).value++;
                tx.openForWrite(ref13, LOCKMODE_NONE).value++;
                tx.openForWrite(ref14, LOCKMODE_NONE).value++;
                tx.openForWrite(ref15, LOCKMODE_NONE).value++;
                tx.openForWrite(ref16, LOCKMODE_NONE).value++;

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

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(betaTransactionConfiguration);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, LOCKMODE_NONE).value++;
                tx.openForWrite(ref2, LOCKMODE_NONE).value++;
                tx.openForWrite(ref3, LOCKMODE_NONE).value++;
                tx.openForWrite(ref4, LOCKMODE_NONE).value++;
                tx.openForWrite(ref5, LOCKMODE_NONE).value++;
                tx.openForWrite(ref6, LOCKMODE_NONE).value++;
                tx.openForWrite(ref7, LOCKMODE_NONE).value++;
                tx.openForWrite(ref8, LOCKMODE_NONE).value++;
                tx.openForWrite(ref9, LOCKMODE_NONE).value++;
                tx.openForWrite(ref10, LOCKMODE_NONE).value++;
                tx.openForWrite(ref11, LOCKMODE_NONE).value++;
                tx.openForWrite(ref12, LOCKMODE_NONE).value++;
                tx.openForWrite(ref13, LOCKMODE_NONE).value++;
                tx.openForWrite(ref14, LOCKMODE_NONE).value++;
                tx.openForWrite(ref15, LOCKMODE_NONE).value++;
                tx.openForWrite(ref16, LOCKMODE_NONE).value++;
                tx.openForWrite(ref17, LOCKMODE_NONE).value++;
                tx.openForWrite(ref18, LOCKMODE_NONE).value++;
                tx.openForWrite(ref19, LOCKMODE_NONE).value++;
                tx.openForWrite(ref20, LOCKMODE_NONE).value++;
                tx.openForWrite(ref21, LOCKMODE_NONE).value++;
                tx.openForWrite(ref22, LOCKMODE_NONE).value++;
                tx.openForWrite(ref23, LOCKMODE_NONE).value++;
                tx.openForWrite(ref24, LOCKMODE_NONE).value++;
                tx.openForWrite(ref25, LOCKMODE_NONE).value++;
                tx.openForWrite(ref26, LOCKMODE_NONE).value++;
                tx.openForWrite(ref27, LOCKMODE_NONE).value++;
                tx.openForWrite(ref28, LOCKMODE_NONE).value++;
                tx.openForWrite(ref29, LOCKMODE_NONE).value++;
                tx.openForWrite(ref30, LOCKMODE_NONE).value++;
                tx.openForWrite(ref31, LOCKMODE_NONE).value++;
                tx.openForWrite(ref32, LOCKMODE_NONE).value++;

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

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(betaTransactionConfiguration);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionsPerThread; iteration++) {
                tx.openForWrite(ref1, LOCKMODE_NONE).value++;
                tx.openForWrite(ref2, LOCKMODE_NONE).value++;
                tx.openForWrite(ref3, LOCKMODE_NONE).value++;
                tx.openForWrite(ref4, LOCKMODE_NONE).value++;
                tx.openForWrite(ref5, LOCKMODE_NONE).value++;
                tx.openForWrite(ref6, LOCKMODE_NONE).value++;
                tx.openForWrite(ref7, LOCKMODE_NONE).value++;
                tx.openForWrite(ref8, LOCKMODE_NONE).value++;
                tx.openForWrite(ref9, LOCKMODE_NONE).value++;
                tx.openForWrite(ref10, LOCKMODE_NONE).value++;
                tx.openForWrite(ref11, LOCKMODE_NONE).value++;
                tx.openForWrite(ref12, LOCKMODE_NONE).value++;
                tx.openForWrite(ref13, LOCKMODE_NONE).value++;
                tx.openForWrite(ref14, LOCKMODE_NONE).value++;
                tx.openForWrite(ref15, LOCKMODE_NONE).value++;
                tx.openForWrite(ref16, LOCKMODE_NONE).value++;
                tx.openForWrite(ref17, LOCKMODE_NONE).value++;
                tx.openForWrite(ref18, LOCKMODE_NONE).value++;
                tx.openForWrite(ref19, LOCKMODE_NONE).value++;
                tx.openForWrite(ref20, LOCKMODE_NONE).value++;
                tx.openForWrite(ref21, LOCKMODE_NONE).value++;
                tx.openForWrite(ref22, LOCKMODE_NONE).value++;
                tx.openForWrite(ref23, LOCKMODE_NONE).value++;
                tx.openForWrite(ref24, LOCKMODE_NONE).value++;
                tx.openForWrite(ref25, LOCKMODE_NONE).value++;
                tx.openForWrite(ref26, LOCKMODE_NONE).value++;
                tx.openForWrite(ref27, LOCKMODE_NONE).value++;
                tx.openForWrite(ref28, LOCKMODE_NONE).value++;
                tx.openForWrite(ref29, LOCKMODE_NONE).value++;
                tx.openForWrite(ref30, LOCKMODE_NONE).value++;
                tx.openForWrite(ref31, LOCKMODE_NONE).value++;
                tx.openForWrite(ref32, LOCKMODE_NONE).value++;
                tx.openForWrite(ref33, LOCKMODE_NONE).value++;
                tx.openForWrite(ref34, LOCKMODE_NONE).value++;
                tx.openForWrite(ref35, LOCKMODE_NONE).value++;
                tx.openForWrite(ref36, LOCKMODE_NONE).value++;
                tx.openForWrite(ref37, LOCKMODE_NONE).value++;
                tx.openForWrite(ref38, LOCKMODE_NONE).value++;
                tx.openForWrite(ref39, LOCKMODE_NONE).value++;
                tx.openForWrite(ref40, LOCKMODE_NONE).value++;
                tx.openForWrite(ref41, LOCKMODE_NONE).value++;
                tx.openForWrite(ref42, LOCKMODE_NONE).value++;
                tx.openForWrite(ref43, LOCKMODE_NONE).value++;
                tx.openForWrite(ref44, LOCKMODE_NONE).value++;
                tx.openForWrite(ref45, LOCKMODE_NONE).value++;
                tx.openForWrite(ref46, LOCKMODE_NONE).value++;
                tx.openForWrite(ref47, LOCKMODE_NONE).value++;
                tx.openForWrite(ref48, LOCKMODE_NONE).value++;
                tx.openForWrite(ref49, LOCKMODE_NONE).value++;
                tx.openForWrite(ref50, LOCKMODE_NONE).value++;
                tx.openForWrite(ref51, LOCKMODE_NONE).value++;
                tx.openForWrite(ref52, LOCKMODE_NONE).value++;
                tx.openForWrite(ref53, LOCKMODE_NONE).value++;
                tx.openForWrite(ref54, LOCKMODE_NONE).value++;
                tx.openForWrite(ref55, LOCKMODE_NONE).value++;
                tx.openForWrite(ref56, LOCKMODE_NONE).value++;
                tx.openForWrite(ref57, LOCKMODE_NONE).value++;
                tx.openForWrite(ref58, LOCKMODE_NONE).value++;
                tx.openForWrite(ref59, LOCKMODE_NONE).value++;
                tx.openForWrite(ref60, LOCKMODE_NONE).value++;
                tx.openForWrite(ref61, LOCKMODE_NONE).value++;
                tx.openForWrite(ref62, LOCKMODE_NONE).value++;
                tx.openForWrite(ref63, LOCKMODE_NONE).value++;
                tx.openForWrite(ref64, LOCKMODE_NONE).value++;

                tx.commit();
                tx.hardReset();

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }

}

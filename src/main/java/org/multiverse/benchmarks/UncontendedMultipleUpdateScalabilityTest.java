package org.multiverse.benchmarks;

import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatArrayBetaTransaction;
import org.multiverse.stms.beta.transactions.FatArrayTreeBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.multiverse.benchmarks.BenchmarkUtils.toGnuplot;
import static org.multiverse.stms.beta.BetaStmUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;

public class UncontendedMultipleUpdateScalabilityTest {

    private BetaStm stm;
    private final long transactionCount = 100 * 1000 * 1000;


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

        double readsPerSecond = BenchmarkUtils.perSecond(
                transactionCount * refCount, totalDurationMs, threadCount);

        System.out.printf("Multiverse> Performance %s writes/second with %s threads\n",
                format(readsPerSecond), threadCount);
        return readsPerSecond;
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
            BetaObjectPool pool = new BetaObjectPool();
            BetaLongRef ref1 = createReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 1);

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForWrite(ref1, false, pool);
                tx.commit(pool);
                tx.hardReset(pool);
            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run2() {
            BetaLongRef ref1 = createReadBiasedLongRef(stm);
            BetaLongRef ref2 = createReadBiasedLongRef(stm);

            BetaObjectPool pool = new BetaObjectPool();

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 1);

            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForWrite(ref1, false, pool);
                tx.openForWrite(ref2, false, pool);
                tx.commit(pool);
                tx.hardReset(pool);

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run4() {
            BetaLongRef ref1 = createReadBiasedLongRef(stm);
            BetaLongRef ref2 = createReadBiasedLongRef(stm);
            BetaLongRef ref3 = createReadBiasedLongRef(stm);
            BetaLongRef ref4 = createReadBiasedLongRef(stm);

            BetaObjectPool pool = new BetaObjectPool();

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForWrite(ref1, false, pool);
                tx.openForWrite(ref2, false, pool);
                tx.openForWrite(ref3, false, pool);
                tx.openForWrite(ref4, false, pool);
                tx.commit(pool);
                tx.hardReset(pool);

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run8() {
            BetaLongRef ref1 = createReadBiasedLongRef(stm);
            BetaLongRef ref2 = createReadBiasedLongRef(stm);
            BetaLongRef ref3 = createReadBiasedLongRef(stm);
            BetaLongRef ref4 = createReadBiasedLongRef(stm);
            BetaLongRef ref5 = createReadBiasedLongRef(stm);
            BetaLongRef ref6 = createReadBiasedLongRef(stm);
            BetaLongRef ref7 = createReadBiasedLongRef(stm);
            BetaLongRef ref8 = createReadBiasedLongRef(stm);

            BetaObjectPool pool = new BetaObjectPool();

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForWrite(ref1, false, pool);
                tx.openForWrite(ref2, false, pool);
                tx.openForWrite(ref3, false, pool);
                tx.openForWrite(ref4, false, pool);
                tx.openForWrite(ref5, false, pool);
                tx.openForWrite(ref6, false, pool);
                tx.openForWrite(ref7, false, pool);
                tx.openForWrite(ref8, false, pool);

                tx.commit(pool);
                tx.hardReset(pool);

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run16() {
            BetaLongRef ref1 = createReadBiasedLongRef(stm);
            BetaLongRef ref2 = createReadBiasedLongRef(stm);
            BetaLongRef ref3 = createReadBiasedLongRef(stm);
            BetaLongRef ref4 = createReadBiasedLongRef(stm);
            BetaLongRef ref5 = createReadBiasedLongRef(stm);
            BetaLongRef ref6 = createReadBiasedLongRef(stm);
            BetaLongRef ref7 = createReadBiasedLongRef(stm);
            BetaLongRef ref8 = createReadBiasedLongRef(stm);
            BetaLongRef ref9 = createReadBiasedLongRef(stm);
            BetaLongRef ref10 = createReadBiasedLongRef(stm);
            BetaLongRef ref11 = createReadBiasedLongRef(stm);
            BetaLongRef ref12 = createReadBiasedLongRef(stm);
            BetaLongRef ref13 = createReadBiasedLongRef(stm);
            BetaLongRef ref14 = createReadBiasedLongRef(stm);
            BetaLongRef ref15 = createReadBiasedLongRef(stm);
            BetaLongRef ref16 = createReadBiasedLongRef(stm);


            BetaObjectPool pool = new BetaObjectPool();

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForWrite(ref1, false, pool);
                tx.openForWrite(ref2, false, pool);
                tx.openForWrite(ref3, false, pool);
                tx.openForWrite(ref4, false, pool);
                tx.openForWrite(ref5, false, pool);
                tx.openForWrite(ref6, false, pool);
                tx.openForWrite(ref7, false, pool);
                tx.openForWrite(ref8, false, pool);
                tx.openForWrite(ref9, false, pool);
                tx.openForWrite(ref10, false, pool);
                tx.openForWrite(ref11, false, pool);
                tx.openForWrite(ref12, false, pool);
                tx.openForWrite(ref13, false, pool);
                tx.openForWrite(ref14, false, pool);
                tx.openForWrite(ref15, false, pool);
                tx.openForWrite(ref16, false, pool);


                tx.commit(pool);
                tx.hardReset(pool);

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run32() {
            BetaLongRef ref1 = createReadBiasedLongRef(stm);
            BetaLongRef ref2 = createReadBiasedLongRef(stm);
            BetaLongRef ref3 = createReadBiasedLongRef(stm);
            BetaLongRef ref4 = createReadBiasedLongRef(stm);
            BetaLongRef ref5 = createReadBiasedLongRef(stm);
            BetaLongRef ref6 = createReadBiasedLongRef(stm);
            BetaLongRef ref7 = createReadBiasedLongRef(stm);
            BetaLongRef ref8 = createReadBiasedLongRef(stm);
            BetaLongRef ref9 = createReadBiasedLongRef(stm);
            BetaLongRef ref10 = createReadBiasedLongRef(stm);
            BetaLongRef ref11 = createReadBiasedLongRef(stm);
            BetaLongRef ref12 = createReadBiasedLongRef(stm);
            BetaLongRef ref13 = createReadBiasedLongRef(stm);
            BetaLongRef ref14 = createReadBiasedLongRef(stm);
            BetaLongRef ref15 = createReadBiasedLongRef(stm);
            BetaLongRef ref16 = createReadBiasedLongRef(stm);
            BetaLongRef ref17 = createReadBiasedLongRef(stm);
            BetaLongRef ref18 = createReadBiasedLongRef(stm);
            BetaLongRef ref19 = createReadBiasedLongRef(stm);
            BetaLongRef ref20 = createReadBiasedLongRef(stm);
            BetaLongRef ref21 = createReadBiasedLongRef(stm);
            BetaLongRef ref22 = createReadBiasedLongRef(stm);
            BetaLongRef ref23 = createReadBiasedLongRef(stm);
            BetaLongRef ref24 = createReadBiasedLongRef(stm);
            BetaLongRef ref25 = createReadBiasedLongRef(stm);
            BetaLongRef ref26 = createReadBiasedLongRef(stm);
            BetaLongRef ref27 = createReadBiasedLongRef(stm);
            BetaLongRef ref28 = createReadBiasedLongRef(stm);
            BetaLongRef ref29 = createReadBiasedLongRef(stm);
            BetaLongRef ref30 = createReadBiasedLongRef(stm);
            BetaLongRef ref31 = createReadBiasedLongRef(stm);
            BetaLongRef ref32 = createReadBiasedLongRef(stm);


            BetaObjectPool pool = new BetaObjectPool();

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForWrite(ref1, false, pool);
                tx.openForWrite(ref2, false, pool);
                tx.openForWrite(ref3, false, pool);
                tx.openForWrite(ref4, false, pool);
                tx.openForWrite(ref5, false, pool);
                tx.openForWrite(ref6, false, pool);
                tx.openForWrite(ref7, false, pool);
                tx.openForWrite(ref8, false, pool);
                tx.openForWrite(ref9, false, pool);
                tx.openForWrite(ref10, false, pool);
                tx.openForWrite(ref11, false, pool);
                tx.openForWrite(ref12, false, pool);
                tx.openForWrite(ref13, false, pool);
                tx.openForWrite(ref14, false, pool);
                tx.openForWrite(ref15, false, pool);
                tx.openForWrite(ref16, false, pool);
                tx.openForWrite(ref17, false, pool);
                tx.openForWrite(ref18, false, pool);
                tx.openForWrite(ref19, false, pool);
                tx.openForWrite(ref20, false, pool);
                tx.openForWrite(ref21, false, pool);
                tx.openForWrite(ref22, false, pool);
                tx.openForWrite(ref23, false, pool);
                tx.openForWrite(ref24, false, pool);
                tx.openForWrite(ref25, false, pool);
                tx.openForWrite(ref26, false, pool);
                tx.openForWrite(ref27, false, pool);
                tx.openForWrite(ref28, false, pool);
                tx.openForWrite(ref29, false, pool);
                tx.openForWrite(ref30, false, pool);
                tx.openForWrite(ref31, false, pool);
                tx.openForWrite(ref32, false, pool);

                tx.commit(pool);
                tx.hardReset(pool);

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }

        public void run64() {
            BetaLongRef ref1 = createReadBiasedLongRef(stm);
            BetaLongRef ref2 = createReadBiasedLongRef(stm);
            BetaLongRef ref3 = createReadBiasedLongRef(stm);
            BetaLongRef ref4 = createReadBiasedLongRef(stm);
            BetaLongRef ref5 = createReadBiasedLongRef(stm);
            BetaLongRef ref6 = createReadBiasedLongRef(stm);
            BetaLongRef ref7 = createReadBiasedLongRef(stm);
            BetaLongRef ref8 = createReadBiasedLongRef(stm);
            BetaLongRef ref9 = createReadBiasedLongRef(stm);
            BetaLongRef ref10 = createReadBiasedLongRef(stm);
            BetaLongRef ref11 = createReadBiasedLongRef(stm);
            BetaLongRef ref12 = createReadBiasedLongRef(stm);
            BetaLongRef ref13 = createReadBiasedLongRef(stm);
            BetaLongRef ref14 = createReadBiasedLongRef(stm);
            BetaLongRef ref15 = createReadBiasedLongRef(stm);
            BetaLongRef ref16 = createReadBiasedLongRef(stm);
            BetaLongRef ref17 = createReadBiasedLongRef(stm);
            BetaLongRef ref18 = createReadBiasedLongRef(stm);
            BetaLongRef ref19 = createReadBiasedLongRef(stm);
            BetaLongRef ref20 = createReadBiasedLongRef(stm);
            BetaLongRef ref21 = createReadBiasedLongRef(stm);
            BetaLongRef ref22 = createReadBiasedLongRef(stm);
            BetaLongRef ref23 = createReadBiasedLongRef(stm);
            BetaLongRef ref24 = createReadBiasedLongRef(stm);
            BetaLongRef ref25 = createReadBiasedLongRef(stm);
            BetaLongRef ref26 = createReadBiasedLongRef(stm);
            BetaLongRef ref27 = createReadBiasedLongRef(stm);
            BetaLongRef ref28 = createReadBiasedLongRef(stm);
            BetaLongRef ref29 = createReadBiasedLongRef(stm);
            BetaLongRef ref30 = createReadBiasedLongRef(stm);
            BetaLongRef ref31 = createReadBiasedLongRef(stm);
            BetaLongRef ref32 = createReadBiasedLongRef(stm);
            BetaLongRef ref33 = createReadBiasedLongRef(stm);
            BetaLongRef ref34 = createReadBiasedLongRef(stm);
            BetaLongRef ref35 = createReadBiasedLongRef(stm);
            BetaLongRef ref36 = createReadBiasedLongRef(stm);
            BetaLongRef ref37 = createReadBiasedLongRef(stm);
            BetaLongRef ref38 = createReadBiasedLongRef(stm);
            BetaLongRef ref39 = createReadBiasedLongRef(stm);
            BetaLongRef ref40 = createReadBiasedLongRef(stm);
            BetaLongRef ref41 = createReadBiasedLongRef(stm);
            BetaLongRef ref42 = createReadBiasedLongRef(stm);
            BetaLongRef ref43 = createReadBiasedLongRef(stm);
            BetaLongRef ref44 = createReadBiasedLongRef(stm);
            BetaLongRef ref45 = createReadBiasedLongRef(stm);
            BetaLongRef ref46 = createReadBiasedLongRef(stm);
            BetaLongRef ref47 = createReadBiasedLongRef(stm);
            BetaLongRef ref48 = createReadBiasedLongRef(stm);
            BetaLongRef ref49 = createReadBiasedLongRef(stm);
            BetaLongRef ref50 = createReadBiasedLongRef(stm);
            BetaLongRef ref51 = createReadBiasedLongRef(stm);
            BetaLongRef ref52 = createReadBiasedLongRef(stm);
            BetaLongRef ref53 = createReadBiasedLongRef(stm);
            BetaLongRef ref54 = createReadBiasedLongRef(stm);
            BetaLongRef ref55 = createReadBiasedLongRef(stm);
            BetaLongRef ref56 = createReadBiasedLongRef(stm);
            BetaLongRef ref57 = createReadBiasedLongRef(stm);
            BetaLongRef ref58 = createReadBiasedLongRef(stm);
            BetaLongRef ref59 = createReadBiasedLongRef(stm);
            BetaLongRef ref60 = createReadBiasedLongRef(stm);
            BetaLongRef ref61 = createReadBiasedLongRef(stm);
            BetaLongRef ref62 = createReadBiasedLongRef(stm);
            BetaLongRef ref63 = createReadBiasedLongRef(stm);
            BetaLongRef ref64 = createReadBiasedLongRef(stm);


            BetaObjectPool pool = new BetaObjectPool();

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
            FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForWrite(ref1, false, pool);
                tx.openForWrite(ref2, false, pool);
                tx.openForWrite(ref3, false, pool);
                tx.openForWrite(ref4, false, pool);
                tx.openForWrite(ref5, false, pool);
                tx.openForWrite(ref6, false, pool);
                tx.openForWrite(ref7, false, pool);
                tx.openForWrite(ref8, false, pool);
                tx.openForWrite(ref9, false, pool);
                tx.openForWrite(ref10, false, pool);
                tx.openForWrite(ref11, false, pool);
                tx.openForWrite(ref12, false, pool);
                tx.openForWrite(ref13, false, pool);
                tx.openForWrite(ref14, false, pool);
                tx.openForWrite(ref15, false, pool);
                tx.openForWrite(ref16, false, pool);
                tx.openForWrite(ref17, false, pool);
                tx.openForWrite(ref18, false, pool);
                tx.openForWrite(ref19, false, pool);
                tx.openForWrite(ref20, false, pool);
                tx.openForWrite(ref21, false, pool);
                tx.openForWrite(ref22, false, pool);
                tx.openForWrite(ref23, false, pool);
                tx.openForWrite(ref24, false, pool);
                tx.openForWrite(ref25, false, pool);
                tx.openForWrite(ref26, false, pool);
                tx.openForWrite(ref27, false, pool);
                tx.openForWrite(ref28, false, pool);
                tx.openForWrite(ref29, false, pool);
                tx.openForWrite(ref30, false, pool);
                tx.openForWrite(ref31, false, pool);
                tx.openForWrite(ref32, false, pool);
                tx.openForWrite(ref33, false, pool);
                tx.openForWrite(ref34, false, pool);
                tx.openForWrite(ref35, false, pool);
                tx.openForWrite(ref36, false, pool);
                tx.openForWrite(ref37, false, pool);
                tx.openForWrite(ref38, false, pool);
                tx.openForWrite(ref39, false, pool);
                tx.openForWrite(ref40, false, pool);
                tx.openForWrite(ref41, false, pool);
                tx.openForWrite(ref42, false, pool);
                tx.openForWrite(ref43, false, pool);
                tx.openForWrite(ref44, false, pool);
                tx.openForWrite(ref45, false, pool);
                tx.openForWrite(ref46, false, pool);
                tx.openForWrite(ref47, false, pool);
                tx.openForWrite(ref48, false, pool);
                tx.openForWrite(ref49, false, pool);
                tx.openForWrite(ref50, false, pool);
                tx.openForWrite(ref51, false, pool);
                tx.openForWrite(ref52, false, pool);
                tx.openForWrite(ref53, false, pool);
                tx.openForWrite(ref54, false, pool);
                tx.openForWrite(ref55, false, pool);
                tx.openForWrite(ref56, false, pool);
                tx.openForWrite(ref57, false, pool);
                tx.openForWrite(ref58, false, pool);
                tx.openForWrite(ref59, false, pool);
                tx.openForWrite(ref60, false, pool);
                tx.openForWrite(ref61, false, pool);
                tx.openForWrite(ref62, false, pool);
                tx.openForWrite(ref63, false, pool);
                tx.openForWrite(ref64, false, pool);

                tx.commit(pool);
                tx.hardReset(pool);

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }

}

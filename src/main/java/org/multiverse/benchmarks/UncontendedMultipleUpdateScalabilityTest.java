package org.multiverse.benchmarks;

import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
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
            LongRef ref1 = createReadBiasedLongRef(stm);

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
            LongRef ref1 = createReadBiasedLongRef(stm);
            LongRef ref2 = createReadBiasedLongRef(stm);

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
            LongRef ref1 = createReadBiasedLongRef(stm);
            LongRef ref2 = createReadBiasedLongRef(stm);
            LongRef ref3 = createReadBiasedLongRef(stm);
            LongRef ref4 = createReadBiasedLongRef(stm);

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
            LongRef ref1 = createReadBiasedLongRef(stm);
            LongRef ref2 = createReadBiasedLongRef(stm);
            LongRef ref3 = createReadBiasedLongRef(stm);
            LongRef ref4 = createReadBiasedLongRef(stm);
            LongRef ref5 = createReadBiasedLongRef(stm);
            LongRef ref6 = createReadBiasedLongRef(stm);
            LongRef ref7 = createReadBiasedLongRef(stm);
            LongRef ref8 = createReadBiasedLongRef(stm);

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
            LongRef ref1 = createReadBiasedLongRef(stm);
            LongRef ref2 = createReadBiasedLongRef(stm);
            LongRef ref3 = createReadBiasedLongRef(stm);
            LongRef ref4 = createReadBiasedLongRef(stm);
            LongRef ref5 = createReadBiasedLongRef(stm);
            LongRef ref6 = createReadBiasedLongRef(stm);
            LongRef ref7 = createReadBiasedLongRef(stm);
            LongRef ref8 = createReadBiasedLongRef(stm);
            LongRef ref9 = createReadBiasedLongRef(stm);
            LongRef ref10 = createReadBiasedLongRef(stm);
            LongRef ref11 = createReadBiasedLongRef(stm);
            LongRef ref12 = createReadBiasedLongRef(stm);
            LongRef ref13 = createReadBiasedLongRef(stm);
            LongRef ref14 = createReadBiasedLongRef(stm);
            LongRef ref15 = createReadBiasedLongRef(stm);
            LongRef ref16 = createReadBiasedLongRef(stm);


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
            LongRef ref1 = createReadBiasedLongRef(stm);
            LongRef ref2 = createReadBiasedLongRef(stm);
            LongRef ref3 = createReadBiasedLongRef(stm);
            LongRef ref4 = createReadBiasedLongRef(stm);
            LongRef ref5 = createReadBiasedLongRef(stm);
            LongRef ref6 = createReadBiasedLongRef(stm);
            LongRef ref7 = createReadBiasedLongRef(stm);
            LongRef ref8 = createReadBiasedLongRef(stm);
            LongRef ref9 = createReadBiasedLongRef(stm);
            LongRef ref10 = createReadBiasedLongRef(stm);
            LongRef ref11 = createReadBiasedLongRef(stm);
            LongRef ref12 = createReadBiasedLongRef(stm);
            LongRef ref13 = createReadBiasedLongRef(stm);
            LongRef ref14 = createReadBiasedLongRef(stm);
            LongRef ref15 = createReadBiasedLongRef(stm);
            LongRef ref16 = createReadBiasedLongRef(stm);
            LongRef ref17 = createReadBiasedLongRef(stm);
            LongRef ref18 = createReadBiasedLongRef(stm);
            LongRef ref19 = createReadBiasedLongRef(stm);
            LongRef ref20 = createReadBiasedLongRef(stm);
            LongRef ref21 = createReadBiasedLongRef(stm);
            LongRef ref22 = createReadBiasedLongRef(stm);
            LongRef ref23 = createReadBiasedLongRef(stm);
            LongRef ref24 = createReadBiasedLongRef(stm);
            LongRef ref25 = createReadBiasedLongRef(stm);
            LongRef ref26 = createReadBiasedLongRef(stm);
            LongRef ref27 = createReadBiasedLongRef(stm);
            LongRef ref28 = createReadBiasedLongRef(stm);
            LongRef ref29 = createReadBiasedLongRef(stm);
            LongRef ref30 = createReadBiasedLongRef(stm);
            LongRef ref31 = createReadBiasedLongRef(stm);
            LongRef ref32 = createReadBiasedLongRef(stm);


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
            LongRef ref1 = createReadBiasedLongRef(stm);
            LongRef ref2 = createReadBiasedLongRef(stm);
            LongRef ref3 = createReadBiasedLongRef(stm);
            LongRef ref4 = createReadBiasedLongRef(stm);
            LongRef ref5 = createReadBiasedLongRef(stm);
            LongRef ref6 = createReadBiasedLongRef(stm);
            LongRef ref7 = createReadBiasedLongRef(stm);
            LongRef ref8 = createReadBiasedLongRef(stm);
            LongRef ref9 = createReadBiasedLongRef(stm);
            LongRef ref10 = createReadBiasedLongRef(stm);
            LongRef ref11 = createReadBiasedLongRef(stm);
            LongRef ref12 = createReadBiasedLongRef(stm);
            LongRef ref13 = createReadBiasedLongRef(stm);
            LongRef ref14 = createReadBiasedLongRef(stm);
            LongRef ref15 = createReadBiasedLongRef(stm);
            LongRef ref16 = createReadBiasedLongRef(stm);
            LongRef ref17 = createReadBiasedLongRef(stm);
            LongRef ref18 = createReadBiasedLongRef(stm);
            LongRef ref19 = createReadBiasedLongRef(stm);
            LongRef ref20 = createReadBiasedLongRef(stm);
            LongRef ref21 = createReadBiasedLongRef(stm);
            LongRef ref22 = createReadBiasedLongRef(stm);
            LongRef ref23 = createReadBiasedLongRef(stm);
            LongRef ref24 = createReadBiasedLongRef(stm);
            LongRef ref25 = createReadBiasedLongRef(stm);
            LongRef ref26 = createReadBiasedLongRef(stm);
            LongRef ref27 = createReadBiasedLongRef(stm);
            LongRef ref28 = createReadBiasedLongRef(stm);
            LongRef ref29 = createReadBiasedLongRef(stm);
            LongRef ref30 = createReadBiasedLongRef(stm);
            LongRef ref31 = createReadBiasedLongRef(stm);
            LongRef ref32 = createReadBiasedLongRef(stm);
            LongRef ref33 = createReadBiasedLongRef(stm);
            LongRef ref34 = createReadBiasedLongRef(stm);
            LongRef ref35 = createReadBiasedLongRef(stm);
            LongRef ref36 = createReadBiasedLongRef(stm);
            LongRef ref37 = createReadBiasedLongRef(stm);
            LongRef ref38 = createReadBiasedLongRef(stm);
            LongRef ref39 = createReadBiasedLongRef(stm);
            LongRef ref40 = createReadBiasedLongRef(stm);
            LongRef ref41 = createReadBiasedLongRef(stm);
            LongRef ref42 = createReadBiasedLongRef(stm);
            LongRef ref43 = createReadBiasedLongRef(stm);
            LongRef ref44 = createReadBiasedLongRef(stm);
            LongRef ref45 = createReadBiasedLongRef(stm);
            LongRef ref46 = createReadBiasedLongRef(stm);
            LongRef ref47 = createReadBiasedLongRef(stm);
            LongRef ref48 = createReadBiasedLongRef(stm);
            LongRef ref49 = createReadBiasedLongRef(stm);
            LongRef ref50 = createReadBiasedLongRef(stm);
            LongRef ref51 = createReadBiasedLongRef(stm);
            LongRef ref52 = createReadBiasedLongRef(stm);
            LongRef ref53 = createReadBiasedLongRef(stm);
            LongRef ref54 = createReadBiasedLongRef(stm);
            LongRef ref55 = createReadBiasedLongRef(stm);
            LongRef ref56 = createReadBiasedLongRef(stm);
            LongRef ref57 = createReadBiasedLongRef(stm);
            LongRef ref58 = createReadBiasedLongRef(stm);
            LongRef ref59 = createReadBiasedLongRef(stm);
            LongRef ref60 = createReadBiasedLongRef(stm);
            LongRef ref61 = createReadBiasedLongRef(stm);
            LongRef ref62 = createReadBiasedLongRef(stm);
            LongRef ref63 = createReadBiasedLongRef(stm);
            LongRef ref64 = createReadBiasedLongRef(stm);


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

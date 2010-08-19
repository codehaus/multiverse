package org.multiverse.benchmarks;

import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.transactions.BetaTransactionConfiguration;
import org.multiverse.stms.beta.transactions.FatArrayBetaTransaction;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import static org.multiverse.benchmarks.BenchmarkUtils.generateProcessorRange;
import static org.multiverse.benchmarks.BenchmarkUtils.toGnuplot;
import static org.multiverse.stms.beta.BetaStmUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.format;

/**
 * @author Peter Veentjer
 */
public class UncontendedMultipleReadScalabilityTest {
    private BetaStm stm;
    private final long transactionCount = 100 * 1000 * 1000;


    public static void main(String[] args) {
        //should be a power of two.
        int refCount = Integer.parseInt(args[0]);
        UncontendedMultipleReadScalabilityTest test = new UncontendedMultipleReadScalabilityTest();

        test.start(refCount);
    }

    public void start(int refCount) {
        int[] processors = generateProcessorRange();

        System.out.printf("Multiverse> Uncontended multiple read transaction benchmark\n");
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

        System.out.printf("Multiverse> Performance %s reads/second with %s threads\n",
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
                default:
                    throw new IllegalStateException();
            }
        }

        public void run1() {
            BetaObjectPool pool = new BetaObjectPool();
            LongRef ref1 = createReadBiasedLongRef(stm);

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 1)
                    .setReadonly(true);
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForRead(ref1, false, pool);
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

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 2)
                    .setReadonly(true);
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForRead(ref1, false, pool);
                tx.openForRead(ref2, false, pool);
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

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 4)
                    .setReadonly(true);
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForRead(ref1, false, pool);
                tx.openForRead(ref2, false, pool);
                tx.openForRead(ref3, false, pool);
                tx.openForRead(ref4, false, pool);
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

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 8)
                    .setReadonly(true);
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForRead(ref1, false, pool);
                tx.openForRead(ref2, false, pool);
                tx.openForRead(ref3, false, pool);
                tx.openForRead(ref4, false, pool);
                tx.openForRead(ref5, false, pool);
                tx.openForRead(ref6, false, pool);
                tx.openForRead(ref7, false, pool);
                tx.openForRead(ref8, false, pool);

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

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 16)
                    .setReadonly(true);
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForRead(ref1, false, pool);
                tx.openForRead(ref2, false, pool);
                tx.openForRead(ref3, false, pool);
                tx.openForRead(ref4, false, pool);
                tx.openForRead(ref5, false, pool);
                tx.openForRead(ref6, false, pool);
                tx.openForRead(ref7, false, pool);
                tx.openForRead(ref8, false, pool);
                tx.openForRead(ref9, false, pool);
                tx.openForRead(ref10, false, pool);
                tx.openForRead(ref11, false, pool);
                tx.openForRead(ref12, false, pool);
                tx.openForRead(ref13, false, pool);
                tx.openForRead(ref14, false, pool);
                tx.openForRead(ref15, false, pool);
                tx.openForRead(ref16, false, pool);


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

            BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 32)
                    .setReadonly(true);
            FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

            long startMs = System.currentTimeMillis();

            for (long iteration = 0; iteration < transactionCount; iteration++) {
                tx.openForRead(ref1, false, pool);
                tx.openForRead(ref2, false, pool);
                tx.openForRead(ref3, false, pool);
                tx.openForRead(ref4, false, pool);
                tx.openForRead(ref5, false, pool);
                tx.openForRead(ref6, false, pool);
                tx.openForRead(ref7, false, pool);
                tx.openForRead(ref8, false, pool);
                tx.openForRead(ref9, false, pool);
                tx.openForRead(ref10, false, pool);
                tx.openForRead(ref11, false, pool);
                tx.openForRead(ref12, false, pool);
                tx.openForRead(ref13, false, pool);
                tx.openForRead(ref14, false, pool);
                tx.openForRead(ref15, false, pool);
                tx.openForRead(ref16, false, pool);
                tx.openForRead(ref17, false, pool);
                tx.openForRead(ref18, false, pool);
                tx.openForRead(ref19, false, pool);
                tx.openForRead(ref20, false, pool);
                tx.openForRead(ref21, false, pool);
                tx.openForRead(ref22, false, pool);
                tx.openForRead(ref23, false, pool);
                tx.openForRead(ref24, false, pool);
                tx.openForRead(ref25, false, pool);
                tx.openForRead(ref26, false, pool);
                tx.openForRead(ref27, false, pool);
                tx.openForRead(ref28, false, pool);
                tx.openForRead(ref29, false, pool);
                tx.openForRead(ref30, false, pool);
                tx.openForRead(ref31, false, pool);
                tx.openForRead(ref32, false, pool);

                tx.commit(pool);
                tx.hardReset(pool);

            }

            durationMs = System.currentTimeMillis() - startMs;
            System.out.printf("Multiverse> %s is finished in %s ms\n", getName(), durationMs);
        }
    }
}

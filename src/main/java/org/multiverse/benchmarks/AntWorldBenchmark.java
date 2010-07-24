package org.multiverse.benchmarks;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.StmUtils;
import org.multiverse.stms.beta.TransactionTemplate;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.transactions.ArrayTreeBetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.BetaTransactionConfig;

import java.util.concurrent.TimeUnit;

import static org.multiverse.benchmarks.BenchmarkUtils.format;

/**
 * @author Peter Veentjer
 */
public class AntWorldBenchmark {

    private BetaStm stm;
    private LongRef[] cells;
    private long transactionCount;
    private int worldSize = 1600;


    @Before
    public void setUp() {
        transactionCount = 2000 * 1000;
        stm = new BetaStm();
        cells = new LongRef[worldSize];

        for (int k = 0; k < cells.length; k++) {
            cells[k] = StmUtils.createLongRef(stm);
        }
    }

    @Test
    public void test() throws InterruptedException {
        SnapshotThread snapshotThread = new SnapshotThread(1,transactionCount );

        snapshotThread.start();
        snapshotThread.join();

        long durationMs = snapshotThread.durationMs;

        for(int k=0;k<worldSize;k++){
            LongRef ref = cells[k];
            //assertSurplus(1, ref);
            //assertUnlocked(ref);
            //assertReadBiased(ref);
        }

        double transactionsPerSecond = BenchmarkUtils.perSecond(transactionCount,durationMs,1);
        System.out.printf("Benchmark took %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationMs));
        System.out.printf("Performance %s snapshots/second\n", format(transactionsPerSecond));
        System.out.printf("Performance %s reads/second\n", format(worldSize*transactionsPerSecond));
    }

    class SnapshotThread extends Thread {

        private final long transactionCount;
        private long durationMs;

        public SnapshotThread(int id, long transactionCount) {
            super("SnapshotThread-" + id);
            this.transactionCount = transactionCount;
        }

        @Override
        public void run() {
            long startMs = System.currentTimeMillis();

            ObjectPool pool = new ObjectPool();

            SnapshotTemplate template = new SnapshotTemplate(cells, pool);

            final long _transactionCount = transactionCount;
            for (int k = 0; k < _transactionCount; k++) {
                template.execute(pool);
            }

            durationMs = System.currentTimeMillis() - startMs;
        }
    }

    class SnapshotTemplate extends TransactionTemplate {

        private final LongRef[] cells;
        private final ObjectPool pool;

        public SnapshotTemplate(LongRef[] cells, ObjectPool pool) {
            super(stm, new ArrayTreeBetaTransaction(
                    new BetaTransactionConfig(stm)
                            .setReadonly(true)
                            .setTrackReadsEnabled(false)));

            this.cells = cells;
            this.pool = pool;
        }

        @Override
        public void execute(BetaTransaction tx) {
            for (int k = 0; k < cells.length; k++) {
                tx.openForRead(cells[k], false, pool);
            }
        }
    }

}

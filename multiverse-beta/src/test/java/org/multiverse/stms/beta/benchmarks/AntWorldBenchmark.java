package org.multiverse.stms.beta.benchmarks;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.AtomicBlock;
import org.multiverse.api.Transaction;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.*;

/**
 * @author Peter Veentjer
 */
public class AntWorldBenchmark implements BetaStmConstants {

    private BetaStm stm;
    private BetaLongRef[] cells;
    private long transactionsPerThread;
    private int worldSize = 1600;


    @Before
    public void setUp() {
        transactionsPerThread = 2000 * 1000;
        stm = new BetaStm();
        cells = new BetaLongRef[worldSize];

        for (int k = 0; k < cells.length; k++) {
            cells[k] = newLongRef(stm);
        }
    }

    @Test
    @Ignore
    public void test() throws InterruptedException {
        SnapshotThread snapshotThread = new SnapshotThread(1, transactionsPerThread);

        snapshotThread.start();
        snapshotThread.join();

        long durationMs = snapshotThread.durationMs;

        for (int k = 0; k < worldSize; k++) {
            BetaLongRef ref = cells[k];
            //assertSurplus(1, ref);
            //assertHasNoCommitLock(ref);
            //assertReadBiased(ref);
        }

        double transactionsPerSecondPerThread = transactionsPerSecondPerThread(transactionsPerThread, durationMs, 1);
        System.out.printf("Benchmark took %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationMs));
        System.out.printf("Performance %s snapshots/second/thread\n", format(transactionsPerSecondPerThread));
        System.out.printf("Performance %s snapshots/second\n",
                transactionsPerSecondAsString(transactionsPerThread, durationMs, 1));
        System.out.printf("Performance %s reads/second\n", format(worldSize * transactionsPerSecondPerThread));
    }

    class SnapshotThread extends TestThread {

        private final long transactionCount;
        private long durationMs;

        public SnapshotThread(int id, long transactionCount) {
            super("SnapshotThread-" + id);
            this.transactionCount = transactionCount;
        }

        @Override
        public void doRun() {
            long startMs = System.currentTimeMillis();

            AtomicBlock block = stm.createTransactionFactoryBuilder()
                    .setBlockingAllowed(false)
                    .setReadonly(true)
                    .setReadTrackingEnabled(false)
                    .buildAtomicBlock();

            AtomicVoidClosure closure = new AtomicVoidClosure() {
                @Override
                public void execute(Transaction tx) throws Exception {
                    BetaTransaction btx = (BetaTransaction) tx;
                    for (int k = 0; k < cells.length; k++) {
                        btx.openForRead(cells[k], LOCKMODE_NONE);
                    }
                }
            };

            final long _transactionCount = transactionCount;
            for (int k = 0; k < _transactionCount; k++) {
                block.execute(closure);
            }

            durationMs = System.currentTimeMillis() - startMs;
        }
    }
}

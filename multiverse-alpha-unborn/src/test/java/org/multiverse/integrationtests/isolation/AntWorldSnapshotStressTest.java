package org.multiverse.integrationtests.isolation;


import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.templates.TransactionTemplate;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A StressTest for stressing the ANT demo provided by clojure. There were some performance problems with it in
 * the Akka project and this test is to figure out what the performance can be if all unwanted stuff is removed.
 *
 * @author Peter Veentjer
 */
public class AntWorldSnapshotStressTest {
    private Stm stm;

    private int width = 80;
    private int height = 80;
    private int testDurationMs = 5 * 60 * 1000;
    private Cell[] cells;

    private volatile boolean stop;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        stop = false;
        stm = getGlobalStmInstance();
        cells = new Cell[width * height];
        new TransactionTemplate() {
            @Override
            public Object execute(Transaction tx) throws Exception {
                for (int k = 0; k < cells.length; k++) {
                    cells[k] = new Cell();
                }
                return null;
            }
        }.execute();
    }

    @Test
    public void test() {
        SnapshotThread snapshotThread = new SnapshotThread();

        long startNs = System.nanoTime();

        startAll(snapshotThread);


        sleepMs(testDurationMs);

        stop = true;
        joinAll(snapshotThread);
        long durationNs = System.nanoTime() - startNs;

        double transactionsPerSecond = (1.0d * snapshotThread.snapshotCount * TimeUnit.SECONDS.toNanos(1)) / durationNs;
        System.out.printf("Performance %s snapshots/second\n", format(transactionsPerSecond));

        double averageTimePerSnapshotNs = snapshotThread.snapshotTimeNs / snapshotThread.snapshotCount;
        System.out.printf("Average time per snapshot is %s ns\n", format(averageTimePerSnapshotNs));
        System.out.printf("Average read access time per cell is %s ns\n", format(averageTimePerSnapshotNs / cells.length));
    }

    class SnapshotThread extends TestThread {

        private long snapshotCount = 0;
        private long snapshotTimeNs = 0;

        public SnapshotThread() {
            super("SnapshotThread");
        }

        @Override
        public void doRun() throws Exception {
            int[] snapshot = new int[cells.length];

            while (!stop) {
                try {
                    long startNs = System.nanoTime();
                    takeSnapshot(snapshot);
                    long durationNs = System.nanoTime() - startNs;
                    snapshotTimeNs += durationNs;
                    snapshotCount++;
                } catch (RuntimeException e) {
                    stop = true;
                    throw e;
                }
            }
        }

        @TransactionalMethod(readonly = true, trackReads = false)
        private void takeSnapshot(int[] snapshot) {
            for (int k = 0; k < snapshot.length; k++) {
                snapshot[k] = cells[k].getValue();
            }
        }
    }

    class AntThread extends TestThread {

        @Override
        public void doRun() throws Exception {
            while (!stop) {

            }
        }
    }

    @TransactionalObject
    class Cell {
        private int value;

        public int getValue() {
            return value;
        }
    }
}
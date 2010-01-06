package org.multiverse.stms.alpha.integrationtests;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;

import java.util.concurrent.TimeUnit;

public class UnsharedDataDoesNotCauseWriteConflictsLongTest {

    private AlphaStm stm;
    private IntRef[] values;
    private int threadCount = Runtime.getRuntime().availableProcessors() * 4;
    private int updateCountPerThread = 2 * 1000 * 1000;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
        //new PrintMultiversionedStmStatisticsThread(multiversionedstm).start();
    }

    @After
    public void tearDown() {
        if (stm.getProfiler() != null) {
            //stm.getProfiler().print();
        }
    }

    @Test
    public void test() {
        createValues();
        TestThread[] writeThreads = createWriteThreads();

        long startNs = System.nanoTime();

        startAll(writeThreads);
        joinAll(writeThreads);

        long periodNs = System.nanoTime() - startNs;
        double transactionPerSecond =
                (updateCountPerThread * threadCount * 1.0d * TimeUnit.SECONDS.toNanos(1)) / periodNs;
        System.out.printf("%s Transaction/second\n", transactionPerSecond);

        assertEquals(0, stm.getProfiler().sumKey1("updatetransaction.failedtoacquirelocks.count"));
        assertEquals(0, stm.getProfiler().sumKey1("updatetransaction.writeconflict.count"));
    }

    private void createValues() {
        values = new IntRef[threadCount];
        for (int k = 0; k < threadCount; k++) {
            values[k] = new IntRef(0);
        }
    }

    private WriteThread[] createWriteThreads() {
        WriteThread[] result = new WriteThread[threadCount];
        for (int k = 0; k < threadCount; k++) {
            result[k] = new WriteThread(k);
        }
        return result;
    }

    private class WriteThread extends TestThread {

        private final int id;

        WriteThread(int id) {
            super("TestThread-" + id);
            this.id = id;
        }

        public void doRun() {
            for (int k = 0; k < updateCountPerThread; k++) {
                if (k % 1000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                doIt();
            }
        }

        @AtomicMethod
        private void doIt() {
            IntRef value = values[id];
            value.inc();
        }
    }
}

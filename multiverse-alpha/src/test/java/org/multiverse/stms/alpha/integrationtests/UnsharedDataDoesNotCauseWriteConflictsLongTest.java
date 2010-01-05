package org.multiverse.stms.alpha.integrationtests;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class UnsharedDataDoesNotCauseWriteConflictsLongTest {

    private AlphaStm stm;
    private TransactionalInteger[] refs;
    private int threadCount = Runtime.getRuntime().availableProcessors() * 4;
    private int updateCountPerThread = 2 * 1000 * 1000;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
        clearThreadLocalTransaction();
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

        //    assertEquals(0, stm.getProfiler().sumKey1("updatetransaction.failedtoacquirelocks.count"));
        //    assertEquals(0, stm.getProfiler().sumKey1("updatetransaction.writeconflict.count"));
    }

    private void createValues() {
        refs = new TransactionalInteger[threadCount];
        for (int k = 0; k < threadCount; k++) {
            refs[k] = new TransactionalInteger(0);
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
                if (k % 200000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                doIt();
            }
        }

        @TransactionalMethod
        private void doIt() {
            TransactionalInteger ref = refs[id];
            ref.inc();
        }
    }
}

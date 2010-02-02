package org.multiverse.benchmarks;

import org.benchy.TestCaseResult;
import org.benchy.executor.AbstractBenchmarkDriver;
import org.benchy.executor.TestCase;
import org.multiverse.TestThread;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class ConcurrentUpdateDriver extends AbstractBenchmarkDriver {

    private int incCountPerThread;
    private int threadCount;
    private IncThread[] threads;
    private AlphaStm stm;
    private TransactionalInteger ref;

    @Override
    public void preRun(TestCase testCase) {
        stm = AlphaStm.createFast();
        setGlobalStmInstance(stm);

        incCountPerThread = testCase.getIntProperty("incCountPerThread");
        threadCount = testCase.getIntProperty("threadCount");

        threads = new IncThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new IncThread(k);
        }

        ref = new TransactionalInteger();
    }

    @Override
    public void run() {
        startAll(threads);
        joinAll(threads);

        assertEquals(incCountPerThread * threadCount, ref.get());
    }

    @Override
    public void postRun(TestCaseResult caseResult) {
        int transactionCount = incCountPerThread * threadCount;
        caseResult.put("transactionCount", transactionCount);

        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1))
                / caseResult.getLongProperty("duration(ns)");
        caseResult.put("transactions/s", transactionsPerSecond);

        double transactionsPerSecondPerThread = transactionsPerSecond / threadCount;
        caseResult.put("transactions/s/thread", transactionsPerSecondPerThread);
    }

    public class IncThread extends TestThread {

        public IncThread(int id) {
            super("IncThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < incCountPerThread; k++) {
                if (k % 1000000 == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }
                ref.inc();
            }
        }
    }
}

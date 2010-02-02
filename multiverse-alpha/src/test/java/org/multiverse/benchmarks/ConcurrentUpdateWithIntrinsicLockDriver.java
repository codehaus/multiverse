package org.multiverse.benchmarks;

import org.benchy.TestCaseResult;
import org.benchy.executor.AbstractBenchmarkDriver;
import org.benchy.executor.TestCase;
import org.multiverse.TestThread;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;

/**
 * @author Peter Veentjer
 */
public class ConcurrentUpdateWithIntrinsicLockDriver extends AbstractBenchmarkDriver {

    private int incCountPerThread;
    private int threadCount;
    private IncThread[] threads;
    private intRef intRef;

    @Override
    public void preRun(TestCase testCase) {
        incCountPerThread = testCase.getIntProperty("incCountPerThread");
        threadCount = testCase.getIntProperty("threadCount");

        threads = new IncThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new IncThread(k);
        }

        intRef = new intRef();
    }

    @Override
    public void run() {
        startAll(threads);
        joinAll(threads);

        assertEquals(incCountPerThread * threadCount, intRef.get());
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
                intRef.inc();
            }
        }
    }

    //traditional object, not an atomic object

    class intRef {
        private int value;

        public synchronized void inc() {
            value++;
        }

        public synchronized int get() {
            return value;
        }
    }
}

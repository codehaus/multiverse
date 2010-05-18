package org.multiverse.benchmarks;

import org.benchy.AbstractBenchmarkDriver;
import org.benchy.TestCase;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.transactional.refs.IntRef;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;

/**
 * @author Peter Veentjer
 */
public class NonConcurrentUpdateDriver extends AbstractBenchmarkDriver {

    private long incCountPerThread;
    private int threadCount;
    private IncThread[] threads;
    private IntRef[] refs;

    @Override
    public void preRun(TestCase testCase) {
        incCountPerThread = testCase.getLongProperty("incCountPerThread");
        threadCount = testCase.getIntProperty("threadCount");

        refs = new IntRef[threadCount];
        threads = new IncThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            refs[k] = new IntRef();
            threads[k] = new IncThread(k, refs[k]);
        }
    }

    @Override
    public void run() {
        startAll(threads);
        joinAll(threads);

        assertEquals(incCountPerThread * threadCount, sum());
    }

    @Override
    public void postRun(TestCaseResult caseResult) {
        long transactionCount = incCountPerThread * threadCount;
        caseResult.put("transactionCount", transactionCount);

        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1))
                / caseResult.getLongProperty("duration(ns)");
        caseResult.put("transactions/second", transactionsPerSecond);

        double transactionsPerSecondPerThread = transactionsPerSecond / threadCount;
        caseResult.put("transactions/s/thread", transactionsPerSecondPerThread);
    }

    private int sum() {
        int result = 0;
        for (IntRef ref : refs) {
            result += ref.get();
        }
        return result;
    }

    public class IncThread extends TestThread {
        private IntRef intRef;


        public IncThread(int id, IntRef intRef) {
            super("IncThread-" + id);
            this.intRef = intRef;
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
}


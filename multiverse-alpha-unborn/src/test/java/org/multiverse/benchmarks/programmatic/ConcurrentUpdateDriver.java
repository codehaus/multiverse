package org.multiverse.benchmarks.programmatic;

import org.benchy.AbstractBenchmarkDriver;
import org.benchy.DriverParameter;
import org.benchy.TestCase;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.api.Stm;
import org.multiverse.api.programmatic.ProgrammaticLong;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ConcurrentUpdateDriver extends AbstractBenchmarkDriver {

    @DriverParameter
    private long incCountPerThread;
    @DriverParameter
    private int threadCount;

    private IncThread[] threads;
    private ProgrammaticLong ref;
    private Stm stm;

    @Override
    public void preRun(TestCase testCase) {
        clearThreadLocalTransaction();

        stm = getGlobalStmInstance();

        threads = new IncThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new IncThread(k);
        }

        ref = stm.getProgrammaticReferenceFactoryBuilder().build().atomicCreateLong(0);
    }

    @Override
    public void run() {
        startAll(threads);
        joinAll(threads);

        assertEquals(incCountPerThread * threadCount, ref.get());
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
                ref.inc(1);
            }
        }
    }
}

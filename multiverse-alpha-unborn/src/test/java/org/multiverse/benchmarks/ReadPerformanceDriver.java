package org.multiverse.benchmarks;

import org.benchy.AbstractBenchmarkDriver;
import org.benchy.TestCase;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.transactional.refs.IntRef;

import java.util.concurrent.TimeUnit;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * A Benchmark that tests read performance of the STM.
 * <p/>
 * Atm only a single atomic object is concurrently read by transactions.
 *
 * @author Peter Veentjer
 */
public class ReadPerformanceDriver extends AbstractBenchmarkDriver {

    private long incCountPerThread;
    private int threadCount;
    private boolean readonly;

    private ReadThread[] threads;
    private IntRef ref;

    @Override
    public void preRun(TestCase testCase) {
        clearThreadLocalTransaction();

        incCountPerThread = testCase.getLongProperty("readCountPerThread");
        threadCount = testCase.getIntProperty("threadCount");
        readonly = testCase.getBooleanProperty("readonly");

        threads = new ReadThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k);
        }

        ref = new IntRef();
    }

    @Override
    public void postRun(TestCaseResult caseResult) {
        long transactionCount = incCountPerThread * threadCount;
        caseResult.put("transactionCount", transactionCount);

        double transactionsPerSecond = (1.0d * transactionCount * TimeUnit.SECONDS.toNanos(1))
                / caseResult.getLongProperty("duration(ns)");
        caseResult.put("transactions/second", transactionsPerSecond);

        double transactionsPerSecondPerThread = transactionsPerSecond / threadCount;
        caseResult.put("transactions/second/thread", transactionsPerSecondPerThread);
    }


    @Override
    public void run() {
        startAll(threads);
        joinAll(threads);
    }

    public class ReadThread extends TestThread {

        public ReadThread(int id) {
            super("ReadThread-" + id);
        }

        @Override
        public void doRun() throws Exception {
            for (int k = 0; k < incCountPerThread; k++) {
                if ((k % 10000000) == 0) {
                    System.out.printf("%s is at %s\n", getName(), k);
                }

                if (readonly) {
                    readInReadonlyMode();
                } else {
                    readInUpdateMode();
                }
            }
        }

        @TransactionalMethod(readonly = true)
        public int readInReadonlyMode() {
            return ref.get();
        }

        @TransactionalMethod(readonly = false)
        public int readInUpdateMode() {
            return ref.get();
        }
    }
}

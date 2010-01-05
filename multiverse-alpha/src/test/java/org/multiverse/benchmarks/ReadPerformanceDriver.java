package org.multiverse.benchmarks;

import org.benchy.executor.AbstractBenchmarkDriver;
import org.benchy.executor.TestCase;
import org.multiverse.TestThread;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.primitives.TransactionalInteger;
import org.multiverse.stms.alpha.AlphaStm;

import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;

/**
 * A Benchmark that tests read performance of the STM.
 *
 * Atm only a single atomic object is concurrently read by transactions.
 *
 * @author Peter Veentjer
 */
public class ReadPerformanceDriver extends AbstractBenchmarkDriver {

    private int incCountPerThread;
    private int threadCount;
    private boolean readonly;

    private ReadThread[] threads;
    private AlphaStm stm;
    private TransactionalInteger ref;

    @Override
    public void preRun(TestCase testCase) {
        stm = AlphaStm.createFast();
        setGlobalStmInstance(stm);

        incCountPerThread = testCase.getIntProperty("readCountPerThread");
        threadCount = testCase.getIntProperty("threadCount");
        readonly = testCase.getBooleanProperty("readonly");

        threads = new ReadThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new ReadThread(k);
        }

        ref = new TransactionalInteger();
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

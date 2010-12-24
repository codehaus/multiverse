package org.multiverse.stms.beta.benchmarks.orec;

import org.benchy.BenchmarkDriver;
import org.benchy.TestCaseResult;
import org.multiverse.TestThread;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.FastOrec;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.benchy.BenchyUtils.format;
import static org.benchy.BenchyUtils.operationsPerSecond;
import static org.benchy.BenchyUtils.operationsPerSecondPerThread;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;


public class OrecNormalNormalUpdateDriver extends BenchmarkDriver implements BetaStmConstants {
    private BetaLongRef ref;
    private GlobalConflictCounter globalConflictCounter;
    private BetaStm stm;

    private int threadCount;
    private long operationCount = 1000 * 1000 * 1000;
    private UpdateThread[] threads;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Operation count is %s\n", operationCount);
        System.out.printf("Multiverse > Thread count is %s\n", threadCount);

        stm = new BetaStm();
        ref = new BetaLongRef(stm);
        globalConflictCounter = stm.getGlobalConflictCounter();

        threads = new UpdateThread[threadCount];
        for (int k = 0; k < threads.length; k++) {
            threads[k] = new UpdateThread(k);
        }
    }

    @Override
    public void run(TestCaseResult testCaseResult) {
        startAll(threads);
        joinAll(threads);
    }

    @Override
    public void processResults(TestCaseResult result) {
        long durationMs = result.getDurationMs();
        double transactionsPerSecond = operationsPerSecond(operationCount, durationMs, threadCount);
        double transactionsPerSecondPerThread = operationsPerSecondPerThread(operationCount, durationMs, threadCount);

        result.put("transactionsPerSecond", transactionsPerSecond);
        result.put("transactionsPerSecondPerThread", transactionsPerSecondPerThread);

        System.out.printf("Performance : %s update-cycles/second\n", format(transactionsPerSecond));
        System.out.printf("Performance : %s update-cycles/second/thread\n", format(transactionsPerSecondPerThread));

    }

    class UpdateThread extends TestThread {
        public UpdateThread(int id) {
            super("id-" + id);
        }

        @Override
        public void doRun() throws Exception {
            final long _cycles = operationCount;
            final FastOrec orec = new FastOrec();
            final BetaLongRef _ref = ref;
            final GlobalConflictCounter _globalGlobalConflictCounter = globalConflictCounter;

            for (long k = 0; k < _cycles; k++) {
                int arriveStatus = orec.___arrive(0);
                if (arriveStatus == ARRIVE_NORMAL) {
                    orec.___tryLockAfterNormalArrive(0, true);
                } else {
                    orec.___tryLockAndArrive(0, true);
                }
                orec.___departAfterUpdateAndUnlock(_globalGlobalConflictCounter, _ref);
            }

            System.out.printf("Orec        : %s\n", orec.___toOrecString());
        }
    }
}

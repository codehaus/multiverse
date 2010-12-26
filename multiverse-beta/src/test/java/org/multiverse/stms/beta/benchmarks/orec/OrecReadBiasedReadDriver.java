package org.multiverse.stms.beta.benchmarks.orec;

import org.benchy.BenchmarkDriver;
import org.benchy.BenchyUtils;
import org.benchy.TestCaseResult;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

public class OrecReadBiasedReadDriver extends BenchmarkDriver implements BetaStmConstants {
    private BetaLongRef ref;
    private GlobalConflictCounter globalConflictCounter;
    private BetaStm stm;

    private long operationCount = 1000 * 1000 * 1000;
    private BetaLongRef orec;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Operation count is %s\n", operationCount);

        stm = new BetaStm();
        ref = new BetaLongRef(stm);
        globalConflictCounter = stm.getGlobalConflictCounter();
        orec = new BetaLongRef(stm);
    }

    @Override
    public void run(TestCaseResult testCaseResult) {
        final long _cycles = operationCount;
        final BetaLongRef _orec = orec;

        for (long k = 0; k < _cycles; k++) {
            int arriveStatus = _orec.___arrive(0);
            if (arriveStatus == ARRIVE_NORMAL) {
                _orec.___departAfterReading();
            }
        }
    }

    @Override
    public void processResults(TestCaseResult result) {
        long durationMs = result.getDurationMs();
        double transactionsPerSecond = BenchyUtils.operationsPerSecond(operationCount, durationMs, 1);

        result.put("transactionsPerSecond", transactionsPerSecond);
        System.out.printf("Performance : %s update-cycles/second\n", transactionsPerSecond);
        System.out.printf("Orec        : %s\n", orec.___toOrecString());
    }

}

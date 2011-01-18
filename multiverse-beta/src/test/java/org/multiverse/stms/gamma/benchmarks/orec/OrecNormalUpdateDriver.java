package org.multiverse.stms.gamma.benchmarks.orec;

import org.benchy.BenchmarkDriver;
import org.benchy.BenchyUtils;
import org.benchy.TestCaseResult;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GlobalConflictCounter;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;

public class OrecNormalUpdateDriver extends BenchmarkDriver implements GammaConstants {
    private GammaLongRef ref;
    private GlobalConflictCounter globalConflictCounter;
    private GammaStm stm;

    private long operationCount = 1000 * 1000 * 1000;
    private GammaLongRef orec;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Operation count is %s\n", operationCount);

        stm = new GammaStm();
        ref = new GammaLongRef(stm);
        globalConflictCounter = stm.getGlobalConflictCounter();
        orec = new GammaLongRef(stm);
    }

    @Override
    public void run(TestCaseResult testCaseResult) {
        final long _cycles = operationCount;
        final GammaLongRef _orec = orec;

        for (long k = 0; k < _cycles; k++) {
            _orec.arrive(1);
            _orec.tryLockAfterNormalArrive(1, LOCKMODE_EXCLUSIVE);
            _orec.departAfterUpdateAndUnlock();
        }
    }

    @Override
    public void processResults(TestCaseResult
                                       result) {
        long durationMs = result.getDurationMs();
        double transactionsPerSecond = BenchyUtils.operationsPerSecond(operationCount, durationMs, 1);

        result.put("transactionsPerSecond", transactionsPerSecond);
        System.out.printf("Performance : %s update-cycles/second\n", BenchyUtils.format(transactionsPerSecond));
        System.out.printf("Orec        : %s\n", orec.___toOrecString());
    }
}

package org.multiverse.stms.beta.benchmarks.orec;

import org.benchy.BenchmarkDriver;
import org.benchy.BenchyUtils;
import org.benchy.TestCaseResult;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.conflictcounters.GlobalConflictCounter;
import org.multiverse.stms.beta.orec.FastOrec;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

public class FastOrecWriteLockUpdateDriver  extends BenchmarkDriver implements BetaStmConstants {
    private BetaLongRef ref;
    private GlobalConflictCounter globalConflictCounter;
    private BetaStm stm;

    private long cycles = 1000 * 1000 * 1000;
    private FastOrec orec;

    @Override
    public void setUp() {
        System.out.printf("Multiverse > Cycles      : %s\n", cycles);

        stm = new BetaStm();
        ref = new BetaLongRef(stm);
        globalConflictCounter = stm.getGlobalConflictCounter();
        orec = new FastOrec();
    }

    @Override
    public void run(TestCaseResult testCaseResult) {
        final long _cycles = cycles;
        final FastOrec _orec = orec;
        final BetaLongRef _ref = ref;
        final GlobalConflictCounter _conflictCounter = globalConflictCounter;

        for (long k = 0; k < _cycles; k++) {
            _orec.___tryLockAndArrive(0, false);
            _orec.___upgradeToCommitLock();
            _orec.___departAfterUpdateAndUnlock(_conflictCounter, _ref);
        }
    }

    @Override
    public void processResults(TestCaseResult
                                       result) {
        long durationMs = result.getDurationMs();
        double transactionsPerSecond = BenchyUtils.operationsPerSecond(cycles, durationMs, 1);

        result.put("transactionsPerSecond", transactionsPerSecond);
        System.out.printf("Performance : %s update-cycles/second\n", transactionsPerSecond);
        System.out.printf("Orec        : %s\n", orec.___toOrecString());
    }
}

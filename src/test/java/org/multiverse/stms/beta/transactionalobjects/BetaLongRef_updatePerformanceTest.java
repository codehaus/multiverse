package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;

public class BetaLongRef_updatePerformanceTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void pessimisticUpdateWithNoDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaObjectPool pool = tx.getPool();

        LongRefTranlocal tranlocal = ref.___newTranlocal();

        long transactionCount = 1000 * 1000 * 1000;
        long startMs = System.currentTimeMillis();
        for (long k = 0; k < transactionCount; k++) {
            ref.___load(2, tx, LOCKMODE_COMMIT, tranlocal);
            tranlocal.setStatus(STATUS_UPDATE);
            tranlocal.setDirty(true);
            ref.___commitDirty(tranlocal, tx, pool);
        }
        long durationMs = System.currentTimeMillis() - startMs;

        System.out.printf("Benchmark took %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationMs));
        System.out.printf("Performance %s updates/second\n",
                transactionsPerSecondAsString(transactionCount, durationMs, 1));

    }

    @Test
    @Ignore
    public void pessimisticUpdateWithDirtyCheck() {
        /*
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaObjectPool pool = tx.getPool();

        long transactionCount = 1000 * 1000 * 1000;
        long startMs = System.currentTimeMillis();
        for (long k = 0; k < transactionCount; k++) {
            LongRefTranlocal tranlocal = ref.___load(2, tx, LOCKMODE_COMMIT,pool);
            tranlocal.isCommitted = false;
            tranlocal.value++;
            tranlocal.calculateIsDirty();
            ref.___commitDirty(tranlocal, tx, pool);
        }
        long durationMs = System.currentTimeMillis() - startMs;

        System.out.printf("Benchmark took %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationMs));
        System.out.printf("Performance %s updates/second\n",
                transactionsPerSecondAsString(transactionCount, durationMs, 1));                    */

    }
}

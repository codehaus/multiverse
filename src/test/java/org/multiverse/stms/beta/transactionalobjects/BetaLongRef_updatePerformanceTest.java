package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import java.util.concurrent.TimeUnit;

import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;

public class BetaLongRef_updatePerformanceTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void pessimisticUpdateWithNoDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaObjectPool pool = tx.getPool();

        long transactionCount = 5l* 1000 * 1000 * 1000;
        long startMs = System.currentTimeMillis();
        for (long k = 0; k < transactionCount; k++) {
            LongRefTranlocal read = ref.___load(2, tx, LOCKMODE_COMMIT);
            LongRefTranlocal write = read.openForWrite(pool);
            ref.___commitAll(write, tx, pool);
        }
        long durationMs = System.currentTimeMillis() - startMs;

        System.out.printf("Benchmark took %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationMs));
        System.out.printf("Performance %s updates/second\n",
                transactionsPerSecondAsString(transactionCount, durationMs, 1));

    }

    @Test
    public void pessimisticUpdateWithDirtyCheck() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaObjectPool pool = tx.getPool();

        long transactionCount = 1000 * 1000 * 1000;
        long startMs = System.currentTimeMillis();
        for (long k = 0; k < transactionCount; k++) {
            LongRefTranlocal read = ref.___load(2, tx, LOCKMODE_COMMIT);
            LongRefTranlocal write = read.openForWrite(pool);
            write.value++;
            write.calculateIsDirty();
            ref.___commitDirty(write, tx, pool);
        }
        long durationMs = System.currentTimeMillis() - startMs;

        System.out.printf("Benchmark took %s ms\n", TimeUnit.NANOSECONDS.toMillis(durationMs));
        System.out.printf("Performance %s updates/second\n",
                transactionsPerSecondAsString(transactionCount, durationMs, 1));

    }
}

package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.fail;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.benchmarks.BenchmarkUtils.transactionsPerSecondAsString;

public class Tranlocal_openBenchmark implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void prepareForPooling() {
        BetaLongRef ref = newLongRef(stm);

        final long count = 1000000000l;
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaObjectPool pool = tx.pool;

        long startMs = System.currentTimeMillis();

        BetaLongRefTranlocal tranlocal = tx.open(ref);
        for (long k = 0; k < count; k++) {
            tranlocal.tx = tx;
            tranlocal.owner = ref;
            tranlocal.prepareForPooling(pool);
            if (tranlocal.owner != null) {
                fail();
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        System.out.printf("Multiverse> Average %s Transactions/second\n",
                transactionsPerSecondAsString(count, durationMs));

    }

    @Test
    public void openForRead() {
        BetaLongRef ref = newLongRef(stm);

        final long count = 1000000000l;
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaObjectPool pool = tx.pool;

        long startMs = System.currentTimeMillis();

        BetaLongRefTranlocal tranlocal = tx.open(ref);
        for (long k = 0; k < count; k++) {
            tranlocal.tx = tx;
            tranlocal.owner = ref;
            tranlocal.openForRead(LOCKMODE_NONE);
            tranlocal.prepareForPooling(pool);
            if (tranlocal.owner != null) {
                fail();
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        System.out.printf("Multiverse> Average %s Transactions/second\n",
                transactionsPerSecondAsString(count, durationMs));

    }

    @Test
    public void openForWrite() {
        BetaLongRef ref = newLongRef(stm);

        final long count = 1000000000l;
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaObjectPool pool = tx.pool;

        long startMs = System.currentTimeMillis();

        BetaLongRefTranlocal tranlocal = tx.open(ref);
        for (long k = 0; k < count; k++) {
            tranlocal.tx = tx;
            tranlocal.owner = ref;
            tranlocal.openForWrite(LOCKMODE_NONE);
            tranlocal.prepareForPooling(pool);
            if (tranlocal.owner != null) {
                fail();
            }
        }

        long durationMs = System.currentTimeMillis() - startMs;
        System.out.printf("Multiverse> Average %s Transactions/second\n",
                transactionsPerSecondAsString(count, durationMs));

    }
}

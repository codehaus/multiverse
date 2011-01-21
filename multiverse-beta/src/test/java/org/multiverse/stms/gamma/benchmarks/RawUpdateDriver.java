package org.multiverse.stms.gamma.benchmarks;

import org.benchy.BenchyUtils;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaObjectPool;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransaction;

public class RawUpdateDriver implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    public static void main(String[] srgs) {
        RawUpdateDriver driver = new RawUpdateDriver();
        driver.setUp();
        driver.testNoLocking();
    }

    @Test
    public void testNoLocking() {
        test(LockMode.None);
    }

    @Test
    public void testReadLock() {
        test(LockMode.Read);
    }

    @Test
    public void testWriteLock() {
        test(LockMode.Write);
    }

    @Test
    public void testWriteCommit() {
        test(LockMode.Exclusive);
    }

    public void test(LockMode writeLockMode) {
        final long txCount = 1000 * 1000 * 1000;

        final FatMonoGammaTransaction tx = new FatMonoGammaTransaction(
                new GammaTransactionConfiguration(stm).setWriteLockMode(writeLockMode));
        final GammaObjectPool pool = tx.pool;

        final GammaLongRef ref = new GammaLongRef(stm, 0);
        final int lockMode = writeLockMode.asInt();
        final GammaRefTranlocal tranlocal = new GammaRefTranlocal();

        final long initialVersion = ref.getVersion();

        final long startMs = System.currentTimeMillis();
        if (lockMode == LOCKMODE_EXCLUSIVE) {
            for (long k = 0; k < txCount; k++) {
                ref.load(tranlocal, LOCKMODE_EXCLUSIVE, 1, false);
                ref.orec = 0;
                //ref.releaseAfterUpdate(tranlocal, pool);
            }

        } else {
            for (long k = 0; k < txCount; k++) {
                ref.load(tranlocal, lockMode, 1, false);
                ref.tryLockAndCheckConflict(1, tranlocal, LOCKMODE_EXCLUSIVE);
                //ref.releaseAfterUpdate(tranlocal, pool);
                ref.orec = 0;
            }
        }
        long durationMs = System.currentTimeMillis() - startMs;

        String s = BenchyUtils.operationsPerSecondPerThreadAsString(txCount, durationMs, 1);

        System.out.printf("Performance is %s transactions/second/thread\n", s);

        //assertEquals(txCount, ref.long_value);
        //assertEquals(txCount + initialVersion, ref.version);
    }
}

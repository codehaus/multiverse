package org.multiverse.stms.beta.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.refs.LongRef;
import org.multiverse.stms.beta.refs.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.MonoBetaTransaction;

import static org.junit.Assert.assertNull;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class ReadBiasedWithPeriodicUpdateTest {

    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void test() {
        LongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.unsafeLoad();

        for (int l = 0; l < 100; l++) {
            BetaTransaction tx = new MonoBetaTransaction(stm);
            tx.openForWrite(ref, false, pool).value++;
            tx.commit();

            for (int k = 0; k < 1000; k++) {
                BetaTransaction readonlyTx = new MonoBetaTransaction(stm);
                readonlyTx.openForRead(ref, false, pool);
                readonlyTx.commit();
            }
        }

        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());

        System.out.println("orec: " + ref.toOrecString());
    }
}

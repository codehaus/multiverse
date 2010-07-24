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
import static org.junit.Assert.assertSame;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertReadBiased;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class ReadBiasedTest {
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

        for(int k=0;k<10000;k++){
            BetaTransaction tx = new MonoBetaTransaction(stm);
            tx.openForRead(ref, false, pool);
            tx.commit();
        }

        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertUnlocked(ref);
        assertNull(ref.getLockOwner());
        assertSame(committed, ref.unsafeLoad());

        System.out.println("orec: "+ref.toOrecString());
    }
}

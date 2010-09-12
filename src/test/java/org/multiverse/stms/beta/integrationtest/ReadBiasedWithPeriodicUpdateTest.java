package org.multiverse.stms.beta.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.assertNull;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class ReadBiasedWithPeriodicUpdateTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void test() {
        BetaLongRef ref = BetaStmUtils.newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        for (int l = 0; l < 100; l++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForWrite(ref, false).value++;
            tx.commit();

            for (int k = 0; k < 1000; k++) {
                BetaTransaction readonlyTx = new FatMonoBetaTransaction(stm);
                readonlyTx.openForRead(ref, false);
                readonlyTx.commit();
            }
        }

        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());

        System.out.println("orec: " + ref.___toOrecString());
    }
}

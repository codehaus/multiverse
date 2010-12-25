package org.multiverse.stms.beta.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertRefHasNoLocks;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertReadBiased;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;

public class ReadBiasedWithPeriodicUpdateTest implements BetaStmConstants {

    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (BetaStm) getGlobalStmInstance();
    }

    @Test
    public void test() {
        BetaLongRef ref = newLongRef(stm);

        for (int l = 0; l < 100; l++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForWrite(ref, LOCKMODE_NONE).value++;
            tx.commit();

            for (int k = 0; k < 1000; k++) {
                BetaTransaction readonlyTx = new FatMonoBetaTransaction(stm);
                readonlyTx.openForRead(ref, LOCKMODE_NONE);
                readonlyTx.commit();
            }
        }

        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertRefHasNoLocks(ref);

        System.out.println("orec: " + ref.___toOrecString());
    }
}

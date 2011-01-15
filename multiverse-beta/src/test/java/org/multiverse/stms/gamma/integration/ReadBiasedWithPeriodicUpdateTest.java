package org.multiverse.stms.gamma.integration;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.MonoGammaTransaction;

import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertReadBiased;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasNoLocks;
import static org.multiverse.stms.gamma.GammaTestUtils.assertSurplus;

public class ReadBiasedWithPeriodicUpdateTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (GammaStm) getGlobalStmInstance();
    }

    @Test
    public void test() {
        GammaLongRef ref = new GammaLongRef(stm);

        for (int l = 0; l < 100; l++) {
            GammaTransaction tx = new MonoGammaTransaction(stm);
            tx.arriveEnabled = true;
            ref.openForWrite(tx, LOCKMODE_NONE).long_value++;
            tx.commit();

            for (int k = 0; k < 1000; k++) {
                GammaTransaction readonlyTx = new MonoGammaTransaction(stm);
                readonlyTx.arriveEnabled = true;
                ref.openForRead(readonlyTx, LOCKMODE_NONE);
                readonlyTx.commit();
            }
        }

        assertSurplus(ref,1);
        assertReadBiased(ref);
        assertRefHasNoLocks(ref);

        System.out.println("orec: " + ref.___toOrecString());
    }
}

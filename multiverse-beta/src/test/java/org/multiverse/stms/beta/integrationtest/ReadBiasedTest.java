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
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertReadBiased;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;

public class ReadBiasedTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (BetaStm) getGlobalStmInstance();
    }

    @Test
    public void test() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        for (int k = 0; k < 10000; k++) {
            BetaTransaction tx = new FatMonoBetaTransaction(stm);
            tx.openForRead(ref, LOCKMODE_NONE);
            tx.commit();
        }

        assertSurplus(1, ref);
        assertReadBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 100);

        System.out.println("orec: " + ref.___toOrecString());
    }
}

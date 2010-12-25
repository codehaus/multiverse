package org.multiverse.stms.beta.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertRefHasNoLocks;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

public class AtomicReadBiasedWithPeriodicUpdateTest {

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
            long value = ref.atomicGet() + 1;
            ref.atomicGetAndSet(value);

            for (int k = 0; k < 1000; k++) {
                long result = ref.atomicGet();
                assertEquals(value, result);
            }
        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);

        System.out.println("orec: " + ref.___toOrecString());
    }
}

package org.multiverse.stms.beta.integrationtest;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.LongRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class AtomicReadBiasedWithPeriodicUpdateTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        LongRef ref = createLongRef(stm);

        for (int l = 0; l < 100; l++) {
            long value = ref.atomicGet() + 1;
            ref.atomicSet(value, pool, 1, stm.getGlobalConflictCounter());

            for (int k = 0; k < 1000; k++) {
                long result = ref.atomicGet();
                assertEquals(value, result);
            }
        }

        //since the value only is returned, we don't need to worry about conflicts.
        //This means that the surplus doesn't need to be increased.
        assertSurplus(0, ref);
        //since no arrive/depart is done, the orec doesn't become read biased based on the
        //atomicget.
        assertUpdateBiased(ref);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());

        System.out.println("orec: " + ref.toOrecString());
    }
}

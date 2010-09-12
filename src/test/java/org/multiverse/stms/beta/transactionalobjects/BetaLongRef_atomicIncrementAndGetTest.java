package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class BetaLongRef_atomicIncrementAndGetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = newLongRef(stm,2);
        long result = ref.atomicIncrementAndGet(10);

        assertEquals(12, result);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    @Ignore
    public void whenActiveTransactionAvailable_thenIgnored(){}

    @Test
    @Ignore
    public void whenNoChange(){}

    @Test
    @Ignore
    public void whenLocked(){

    }
}

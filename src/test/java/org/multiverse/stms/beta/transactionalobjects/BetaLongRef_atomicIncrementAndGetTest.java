package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class BetaLongRef_atomicIncrementAndGetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = newLongRef(stm, 2);
        long result = ref.atomicIncrementAndGet(10);

        assertEquals(12, result);
        assertUnlocked(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(tx, 1000);

        long result = ref.atomicIncrementAndGet(1);

        assertEquals(1, result);
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertNull(ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(1, ref.atomicGet());
    }

    @Test
    @Ignore
    public void whenListenersAvailable(){}

    @Test
    @Ignore
    public void whenNoChange() {
    }

    @Test
    @Ignore
    public void whenLocked() {

    }
}

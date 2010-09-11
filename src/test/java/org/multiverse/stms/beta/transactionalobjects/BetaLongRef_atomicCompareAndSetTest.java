package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class BetaLongRef_atomicCompareAndSetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenLocked() {

    }

    @Test
    @Ignore
    public void whenActiveTransactionAvailable_thenIgnored() {

    }

    @Test
    public void whenExpectedValueFoundAndUpdateIsSame() {
        BetaLongRef ref = createLongRef(stm, 1);
        LongRefTranlocal committed = ref.___unsafeLoad();

        boolean result = ref.atomicCompareAndSet(1, 1);

        assertTrue(result);
        assertEquals(1, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertSurplus(0, ref);
    }

    @Test
    public void whenExpectedValueFound() {
        BetaLongRef ref = createLongRef(stm, 1);
        LongRefTranlocal committed = ref.___unsafeLoad();

        boolean result = ref.atomicCompareAndSet(1, 2);

        assertTrue(result);
        assertEquals(2, ref.atomicGet());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertNotSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenExpectedValueNotFound() {
        BetaLongRef ref = createLongRef(stm, 2);
        LongRefTranlocal committed = ref.___unsafeLoad();

        boolean result = ref.atomicCompareAndSet(1, 3);

        assertFalse(result);
        assertEquals(2, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertSurplus(0, ref);
    }


    @Test
    @Ignore
    public void whenListenersAvailable() {

    }
}

package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.LockedException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_atomicCompareAndSetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenLocked() {
        BetaLongRef ref = newLongRef(stm, 1);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.openForRead(ref, true);

        try {
            ref.atomicCompareAndSet(1, 2);
            fail();
        } catch (LockedException expected) {

        }

        assertSurplus(1, ref);
        assertLocked(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = newLongRef(stm, 1);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(20);

        boolean result = ref.atomicCompareAndSet(1, 2);

        assertTrue(result);
        assertEquals(2, ref.atomicGet());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertNotSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenExpectedValueFoundAndUpdateIsSame() {
        BetaLongRef ref = newLongRef(stm, 1);
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
        BetaLongRef ref = newLongRef(stm, 1);
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
        BetaLongRef ref = newLongRef(stm, 2);
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

package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class BetaLongRef_atomicGetAndIncrementTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 2);

        long result = ref.atomicGetAndIncrement(1);
        assertEquals(2, result);
        assertEquals(3, ref.atomicGet());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = createLongRef(stm, 2);
        LongRefTranlocal committed = ref.___unsafeLoad();

        long result = ref.atomicGetAndIncrement(0);

        assertEquals(2, result);
        assertEquals(2, ref.atomicGet());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored() {
        BetaLongRef ref = createLongRef(stm, 2);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(10);

        long result = ref.atomicGetAndIncrement(1);

        assertEquals(2, result);
        assertEquals(3, ref.atomicGet());
        assertUnlocked(ref);
        assertSurplus(1, ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {

    }

    @Test
    @Ignore
    public void whenLocked() {

    }
}

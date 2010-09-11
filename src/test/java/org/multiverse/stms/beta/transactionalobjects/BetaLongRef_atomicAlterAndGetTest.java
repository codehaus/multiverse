package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUnlocked;

public class BetaLongRef_atomicAlterAndGetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenFunctionCausesException() {
        BetaLongRef ref = createLongRef(stm);

        LongFunction function = mock(LongFunction.class);
        RuntimeException ex = new RuntimeException();
        when(function.call(anyLong())).thenThrow(ex);

        try {
            ref.atomicAlterAndGet(function);
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertEquals(0, ref.atomicGet());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm, 5);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.atomicAlterAndGet(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertEquals(5, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 5);
        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;

        long result = ref.atomicAlterAndGet(function);

        assertEquals(6, result);
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(6, ref.atomicGet());
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {

    }

    @Test
    public void whenNoChange() {
        BetaLongRef ref = createLongRef(stm, 5);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = new IdentityLongFunction();

        long result = ref.atomicAlterAndGet(function);

        assertEquals(5, result);
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(5, ref.atomicGet());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    @Ignore
    public void whenActiveTransactionAvailable_thenIgnored() {

    }

    @Test
    @Ignore
    public void whenLocked() {

    }


}

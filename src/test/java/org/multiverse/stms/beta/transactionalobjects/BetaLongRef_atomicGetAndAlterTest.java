package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_atomicGetAndAlterTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = createLongRef(stm, 2);

        LongFunction function = IncLongFunction.INSTANCE_INC_ONE;
        long result = ref.atomicGetAndAlter(function);

        assertEquals(2, result);
        assertUnlocked(ref);
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertEquals(3, ref.atomicGet());
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = createLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.atomicGetAndAlter(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSame(committed, ref.___unsafeLoad());
        assertUnlocked(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___getLockOwner());
        assertEquals(0, ref.atomicGet());
    }

    @Test
    @Ignore
    public void whenActiveTransactionAvailable_thenIgnored(){

    }
    
    @Test
    @Ignore
    public void whenLocked(){

    }
}

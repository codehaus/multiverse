package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.newReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertReadBiased;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_atomicGetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = IllegalStateException.class)
    public void whenUnconstructed() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        ref.atomicGet();
    }

    @Test
    public void whenActiveTransactionAvailable_thenIgnored(){
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.set(10);

        assertEquals(100, ref.atomicGet());

        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenUpdatedBiasedOnUnlocked() {
        BetaLongRef ref = newLongRef(stm, 100);

        long result = ref.atomicGet();
        assertEquals(100, result);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdateBiasedAndLocked_thenIllegalStateException() {
        BetaLongRef ref = newLongRef(stm, 100);
        BetaTransaction lockOwner = stm.startDefaultTransaction();
        lockOwner.openForRead(ref, true);

        try {
            ref.atomicGet();
            fail();
        } catch (IllegalStateException ex) {

        }

        assertSame(lockOwner, ref.___getLockOwner());
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        BetaLongRef ref = newReadBiasedLongRef(stm, 100);

        long result = ref.atomicGet();
        assertEquals(100, result);
        assertReadBiased(ref);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        BetaLongRef ref = newReadBiasedLongRef(stm, 100);
        BetaTransaction lockOwner = stm.startDefaultTransaction();
        lockOwner.openForRead(ref, true);

        try {
            ref.atomicGet();
            fail();
        } catch (IllegalStateException expected) {

        }

        assertSame(lockOwner, ref.___getLockOwner());
        assertReadBiased(ref);
    }
}

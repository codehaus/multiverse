package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.BetaStmUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertReadBiased;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

/**
 * @author Peter Veentjer
 */
public class LongRef_atomicGetTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test(expected = IllegalStateException.class)
    public void whenUnconstructed() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LongRef ref = new LongRef(tx);
        ref.atomicGet();
    }

    @Test
    public void whenUpdatedBiasedOnUnlocked() {
        LongRef ref = createLongRef(stm, 100);

        long result = ref.atomicGet();
        assertEquals(100, result);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdateBiasedAndLocked() {
        LongRef ref = createLongRef(stm, 100);
        BetaTransaction lockOwner = stm.startDefaultTransaction();
        lockOwner.openForRead(ref, true, new BetaObjectPool());

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSame(lockOwner, ref.___getLockOwner());
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadBiasedAndUnlocked() {
        LongRef ref = createReadBiasedLongRef(stm, 100);

        long result = ref.atomicGet();
        assertEquals(100, result);
        assertReadBiased(ref);
    }

    @Test
    public void whenReadBiasedAndLocked() {
        LongRef ref = createReadBiasedLongRef(stm, 100);
        BetaTransaction lockOwner = stm.startDefaultTransaction();
        lockOwner.openForRead(ref, true, new BetaObjectPool());

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSame(lockOwner, ref.___getLockOwner());
        assertReadBiased(ref);
    }
}

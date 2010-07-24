package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.StmUtils.createLongRef;
import static org.multiverse.stms.beta.StmUtils.createReadBiasedLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertReadBiased;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

/**
 * @author Peter Veentjer
 */
public class LongRef_atomicGetTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
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
        BetaTransaction lockOwner = stm.start();
        lockOwner.openForRead(ref, true, new ObjectPool());

        try {
            ref.atomicGet();
            fail();
        } catch (ReadConflict ex) {
        }

        assertSame(lockOwner, ref.getLockOwner());
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
        BetaTransaction lockOwner = stm.start();
        lockOwner.openForRead(ref, true, new ObjectPool());

        long result = ref.atomicGet();

        assertEquals(100, result);
        assertSame(lockOwner, ref.getLockOwner());
        assertReadBiased(ref);
    }
}

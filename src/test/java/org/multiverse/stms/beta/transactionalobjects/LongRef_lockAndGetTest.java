package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertLocked;

public class LongRef_lockAndGetTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenUnlocked() {
        BetaLongRef ref = createLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        long value = ref.lockAndGet(tx, pool);
        assertEquals(10, value);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);

        tx.commit();

    }

    @Test
    public void whenAlreadyLockedBySelf() {
        BetaLongRef ref = createLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        ref.lockAndGet(tx, pool);
        long value = ref.lockAndGet(tx, pool);
        assertEquals(10, value);
        assertSame(tx, ref.___getLockOwner());
        assertLocked(ref);

        tx.commit();
    }

    @Test
    public void whenAlreadyLockedByOther() {
        BetaLongRef ref = createLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        ref.lockAndGet(otherTx, pool);
        try {
            ref.lockAndGet(tx, pool);
            fail();
        } catch (ReadConflict e) {

        }
        assertSame(otherTx, ref.___getLockOwner());
        assertLocked(ref);
    }
}

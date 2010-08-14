package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;
import org.multiverse.stms.beta.transactions.FatMonoBetaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRef_hasReadConflictTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenReadAndNoConflict() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false, pool);

        boolean hasReadConflict = ref.hasReadConflict(read, tx);

        assertFalse(hasReadConflict);
    }

    @Test
    public void whenWriteAndNoConflict() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false, pool);

        boolean hasReadConflict = ref.hasReadConflict(write, tx);

        assertFalse(hasReadConflict);
    }


    @Test
    public void whenLockedBySelf() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        Tranlocal read = tx.openForRead(ref, true, pool);

        boolean hasConflict = ref.hasReadConflict(read, tx);

        assertFalse(hasConflict);
    }

    @Test
    public void whenUpdatedByOther() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        Tranlocal read = tx.openForRead(ref, false, pool);

        //do the update
        BetaTransaction updatingTx = new FatMonoBetaTransaction(stm);
        updatingTx.openForWrite(ref, false, pool).value++;
        updatingTx.commit();

        boolean hasConflict = ref.hasReadConflict(read, tx);
        assertTrue(hasConflict);
    }

    @Test
    @Ignore
    public void whenFresh() {

    }

    @Test
    public void whenValueChangedByOtherAndLocked() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        Tranlocal read = tx.openForRead(ref, false, pool);

        //do the update
        BetaTransaction updatingTx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false, pool).value++;
        updatingTx.commit();

        //lock it
        BetaTransaction lockingTx = new FatMonoBetaTransaction(stm);
        lockingTx.openForRead(ref, true, pool);

        boolean hasConflict = ref.hasReadConflict(read, tx);
        assertTrue(hasConflict);
    }

    @Test
    public void whenUpdateInProgressBecauseLockedByOther() {
        LongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForRead(ref, false, pool);

        BetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        boolean hasReadConflict = ref.hasReadConflict(write, tx);

        assertTrue(hasReadConflict);
    }
}

package org.multiverse.stms.beta.transactionalobjects;

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

public class BetaLongRef_hasReadConflictTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenReadAndNoConflict() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);

        boolean hasReadConflict = ref.___hasReadConflict(read, tx);

        assertFalse(hasReadConflict);
    }

    @Test
    public void whenWriteAndNoConflict() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false);

        boolean hasReadConflict = ref.___hasReadConflict(write, tx);

        assertFalse(hasReadConflict);
    }


    @Test
    public void whenLockedBySelf() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        Tranlocal read = tx.openForRead(ref, true);

        boolean hasConflict = ref.___hasReadConflict(read, tx);

        assertFalse(hasConflict);
    }

    @Test
    public void whenUpdatedByOther() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        Tranlocal read = tx.openForRead(ref, false);

        //do the update
        BetaTransaction updatingTx = new FatMonoBetaTransaction(stm);
        updatingTx.openForWrite(ref, false).value++;
        updatingTx.commit();

        boolean hasConflict = ref.___hasReadConflict(read, tx);
        assertTrue(hasConflict);
    }

    @Test
    @Ignore
    public void whenFresh() {

    }

    @Test
    public void whenValueChangedByOtherAndLocked() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        Tranlocal read = tx.openForRead(ref, false);

        //do the update
        BetaTransaction updatingTx = new FatMonoBetaTransaction(stm);
        tx.openForWrite(ref, false).value++;
        updatingTx.commit();

        //lock it
        BetaTransaction lockingTx = new FatMonoBetaTransaction(stm);
        lockingTx.openForRead(ref, true);

        boolean hasConflict = ref.___hasReadConflict(read, tx);
        assertTrue(hasConflict);
    }

    @Test
    public void whenUpdateInProgressBecauseLockedByOther() {
        BetaLongRef ref = createLongRef(stm);

        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForRead(ref, false);

        BetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true);

        boolean hasReadConflict = ref.___hasReadConflict(write, tx);

        assertTrue(hasReadConflict);
    }
}

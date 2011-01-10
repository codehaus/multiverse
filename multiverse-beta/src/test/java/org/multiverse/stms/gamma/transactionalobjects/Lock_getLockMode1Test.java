package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;

public class Lock_getLockMode1Test {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTransaction() {
        GammaLongRef ref = new GammaLongRef(stm);

        ref.getLock().getLockMode(null);
    }

    @Test
    public void other_whenLockedForReadByOther() {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = stm.startDefaultTransaction();
        LockMode result = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, result);
    }

    @Test
    public void other_whenLockedForWriteByOther() {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = stm.startDefaultTransaction();
        LockMode result = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, result);
    }

    @Test
    public void other_whenLockForCommitByOther() {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        GammaTransaction tx = stm.startDefaultTransaction();
        LockMode result = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, result);
    }

    @Test
    public void self_whenNotReadBySelf() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, lockMode);
        assertIsActive(tx);
        assertNull(tx.locate(ref));
    }

    @Test
    public void self_whenNotLockedBySelf() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, lockMode);
        assertIsActive(tx);
    }

    @Test
    public void self_whenReadLockedByself() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.Read, lockMode);
        assertIsActive(tx);
    }

    @Test
    public void self_whenWriteLockedBySelf() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.Write, lockMode);
        assertIsActive(tx);
    }

    @Test
    public void self_whenCommitLockedBySelf() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Commit);
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.Commit, lockMode);
        assertIsActive(tx);
    }

    // ====================== states ==================================

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.getLock().getLockMode(tx);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.getLock().getLockMode(tx);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.getLock().getLockMode(tx);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
    }
}

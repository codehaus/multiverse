package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;

public class Lock_atomicGetLockModeTest {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenFree() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        LockMode lockMode = ref.atomicGetLockMode();
        assertEquals(LockMode.None, lockMode);
    }

    @Test
    public void whenReadLocked() {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Read);

        LockMode lockMode = ref.atomicGetLockMode();
        assertEquals(LockMode.Read, lockMode);
    }

    @Test
    public void whenWriteLocked() {
        GammaLongRef ref = new GammaLongRef(stm, 0);
        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Write);

        LockMode lockMode = ref.atomicGetLockMode();
        assertEquals(LockMode.Write, lockMode);
    }

    @Test
    public void whenExclusiveLocked() {
        GammaLongRef ref = new GammaLongRef(stm, 0);
        GammaTransaction tx = stm.startDefaultTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);

        LockMode lockMode = ref.atomicGetLockMode();
        assertEquals(LockMode.Exclusive, lockMode);
    }
}

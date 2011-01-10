package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class Orec_tryUpgradeFromReadLockTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenNoLockAcquiredAndUpgradeToWriteLock_thenPanicError() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        try {
            ref.tryUpgradeFromReadLock(1, false);
            fail();
        } catch (PanicError expected) {
        }

        assertLockMode(ref, LOCKMODE_NONE);
        assertReadLockCount(ref, 0);
        assertSurplus(0, ref);
    }

     @Test
    public void whenNoLockAcquiredAndUpgradeToCommitLock_thenPanicError() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        try {
            ref.tryUpgradeFromReadLock(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertLockMode(ref, LOCKMODE_NONE);
        assertReadLockCount(ref, 0);
        assertSurplus(0, ref);
    }

    @Test
    public void whenSingleReadLockAcquiredAndUpgradeToWriteLock_thenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        ref.openForRead(stm.startDefaultTransaction(), LOCKMODE_READ);

        boolean result = ref.tryUpgradeFromReadLock(1, false);
        assertTrue(result);
        assertTrue(ref.hasWriteLock());
        assertReadLockCount(ref, 0);
        assertSurplus(1, ref);
    }

    @Test
    public void whenSingleReadLockAcquiredAndUpgradeToCommitLock_thenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        ref.openForRead(stm.startDefaultTransaction(), LOCKMODE_READ);

        boolean result = ref.tryUpgradeFromReadLock(1, true);
        assertTrue(result);
        assertTrue(ref.hasCommitLock());
        assertReadLockCount(ref, 0);
        assertSurplus(1, ref);
    }

    @Test
    public void whenMultipleReadLocksAcquired_thenUpgradeToWriteLockFailure() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        ref.openForRead(stm.startDefaultTransaction(), LOCKMODE_READ);
        ref.openForRead(stm.startDefaultTransaction(), LOCKMODE_READ);

        boolean result = ref.tryUpgradeFromReadLock(1, false);
        assertFalse(result);
        assertReadLockCount(ref, 2);
        assertSurplus(2, ref);
    }

    @Test
    public void whenMultipleReadLocksAcquired_thenUpgradeToCommitLockFailure() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        ref.openForRead(stm.startDefaultTransaction(), LOCKMODE_READ);
        ref.openForRead(stm.startDefaultTransaction(), LOCKMODE_READ);

        boolean result = ref.tryUpgradeFromReadLock(1, true);
        assertFalse(result);
        assertReadLockCount(ref, 2);
        assertSurplus(2, ref);
    }

    @Test
    public void whenWriteLockAcquired_thenPanicError() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.openForRead(tx, LOCKMODE_WRITE);

        try {
            ref.tryUpgradeFromReadLock(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertTrue(ref.hasWriteLock());
        assertReadLockCount(ref, 0);
        assertSurplus(1, ref);
    }

    @Test
    public void whenCommitLockAcquired_thenPanicError() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.openForRead(tx, LOCKMODE_COMMIT);

        try {
            ref.tryUpgradeFromReadLock(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertTrue(ref.hasCommitLock());
        assertReadLockCount(ref, 0);
        assertSurplus(1, ref);
    }
}

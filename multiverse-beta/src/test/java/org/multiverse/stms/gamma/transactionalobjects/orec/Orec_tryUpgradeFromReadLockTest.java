package org.multiverse.stms.gamma.transactionalobjects.orec;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PanicError;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
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
            ref.tryUpgradeReadLockToWriteOrExclusiveLock(1, false);
            fail();
        } catch (PanicError expected) {
        }

        assertLockMode(ref, LOCKMODE_NONE);
        assertReadLockCount(ref, 0);
        assertSurplus(ref, 0);
    }

    @Test
    public void whenNoLockAcquiredAndUpgradeToExclusiveLock_thenPanicError() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        try {
            ref.tryUpgradeReadLockToWriteOrExclusiveLock(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertLockMode(ref, LOCKMODE_NONE);
        assertReadLockCount(ref, 0);
        assertSurplus(ref, 0);
    }

    @Test
    public void whenSingleReadLockAcquiredAndUpgradeToWriteLock_thenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        ref.openForRead(stm.newDefaultTransaction(), LOCKMODE_READ);

        boolean result = ref.tryUpgradeReadLockToWriteOrExclusiveLock(1, false);
        assertTrue(result);
        assertTrue(ref.hasWriteLock());
        assertReadLockCount(ref, 0);
        assertSurplus(ref, 1);
    }

    @Test
    public void whenSingleReadLockAcquiredAndUpgradeToExclusiveLock_thenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        ref.openForRead(stm.newDefaultTransaction(), LOCKMODE_READ);

        boolean result = ref.tryUpgradeReadLockToWriteOrExclusiveLock(1, true);
        assertTrue(result);
        assertTrue(ref.hasExclusiveLock());
        assertReadLockCount(ref, 0);
        assertSurplus(ref, 1);
    }

    @Test
    public void whenMultipleReadLocksAcquired_thenUpgradeToWriteLockFailure() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        ref.openForRead(stm.newDefaultTransaction(), LOCKMODE_READ);
        ref.openForRead(stm.newDefaultTransaction(), LOCKMODE_READ);

        boolean result = ref.tryUpgradeReadLockToWriteOrExclusiveLock(1, false);
        assertFalse(result);
        assertReadLockCount(ref, 2);
        assertSurplus(ref, 2);
    }

    @Test
    public void whenMultipleReadLocksAcquired_thenUpgradeToExclusiveLockFailure() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        ref.openForRead(stm.newDefaultTransaction(), LOCKMODE_READ);
        ref.openForRead(stm.newDefaultTransaction(), LOCKMODE_READ);

        boolean result = ref.tryUpgradeReadLockToWriteOrExclusiveLock(1, true);
        assertFalse(result);
        assertReadLockCount(ref, 2);
        assertSurplus(ref, 2);
    }

    @Test
    public void whenWriteLockAcquired_thenPanicError() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        GammaTransaction tx = stm.newDefaultTransaction();
        ref.openForRead(tx, LOCKMODE_WRITE);

        try {
            ref.tryUpgradeReadLockToWriteOrExclusiveLock(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertTrue(ref.hasWriteLock());
        assertReadLockCount(ref, 0);
        assertSurplus(ref, 1);
    }

    @Test
    public void whenExclusiveLockAcquired_thenPanicError() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        GammaTransaction tx = stm.newDefaultTransaction();
        ref.openForRead(tx, LOCKMODE_EXCLUSIVE);

        try {
            ref.tryUpgradeReadLockToWriteOrExclusiveLock(1, true);
            fail();
        } catch (PanicError expected) {
        }

        assertTrue(ref.hasExclusiveLock());
        assertReadLockCount(ref, 0);
        assertSurplus(ref, 1);
    }
}
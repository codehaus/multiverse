package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.stms.gamma.GammaTestUtils.assertLockMode;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasNoLocks;

/**
 * @author Peter Veentjer
 */
public class Lock_tryAcquire1Test {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    @Ignore
    public void test() {

    }

    @Test
    public void whenLockAvailableButConflictFound() {

    }

    @Test
    public void whenLockedByOther() {
        whenLockedByOther(LockMode.None, LockMode.None, true, LockMode.None);
        whenLockedByOther(LockMode.None, LockMode.Read, true, LockMode.Read);
        whenLockedByOther(LockMode.None, LockMode.Write, true, LockMode.Write);
        whenLockedByOther(LockMode.None, LockMode.Commit, true, LockMode.Commit);

        whenLockedByOther(LockMode.Read, LockMode.None, true, LockMode.None);
        whenLockedByOther(LockMode.Read, LockMode.Read, true, LockMode.Read);
        whenLockedByOther(LockMode.Read, LockMode.Write, false, LockMode.Read);
        whenLockedByOther(LockMode.Read, LockMode.Commit, false, LockMode.Read);

        whenLockedByOther(LockMode.Write, LockMode.None, false, LockMode.Write);
        whenLockedByOther(LockMode.Write, LockMode.Read, false, LockMode.Write);
        whenLockedByOther(LockMode.Write, LockMode.Write, false, LockMode.Write);
        whenLockedByOther(LockMode.Write, LockMode.Commit, false, LockMode.Write);

        whenLockedByOther(LockMode.Commit, LockMode.None, false, LockMode.Commit);
        whenLockedByOther(LockMode.Commit, LockMode.Read, false, LockMode.Commit);
        whenLockedByOther(LockMode.Commit, LockMode.Write, false, LockMode.Commit);
        whenLockedByOther(LockMode.Commit, LockMode.Commit, false, LockMode.Commit);
    }

    public void whenLockedByOther(LockMode otherLockMode, LockMode selfLockMode, boolean success, LockMode expectedLockMode) {

    }

    @Test
    public void whenNotLockedByOther() {
        whenNotLockedByOther(LockMode.None, LockMode.None, LockMode.None);
        whenNotLockedByOther(LockMode.None, LockMode.Read, LockMode.Read);
        whenNotLockedByOther(LockMode.None, LockMode.Write, LockMode.Write);
        whenNotLockedByOther(LockMode.None, LockMode.Commit, LockMode.Commit);

        whenNotLockedByOther(LockMode.Read, LockMode.None, LockMode.Read);
        whenNotLockedByOther(LockMode.Read, LockMode.Read, LockMode.Read);
        whenNotLockedByOther(LockMode.Read, LockMode.Write, LockMode.Write);
        whenNotLockedByOther(LockMode.Read, LockMode.Commit, LockMode.Commit);

        whenNotLockedByOther(LockMode.Write, LockMode.None, LockMode.Write);
        whenNotLockedByOther(LockMode.Write, LockMode.Read, LockMode.Write);
        whenNotLockedByOther(LockMode.Write, LockMode.Write, LockMode.Write);
        whenNotLockedByOther(LockMode.Write, LockMode.Commit, LockMode.Commit);

        whenNotLockedByOther(LockMode.Commit, LockMode.None, LockMode.Commit);
        whenNotLockedByOther(LockMode.Commit, LockMode.Read, LockMode.Commit);
        whenNotLockedByOther(LockMode.Commit, LockMode.Write, LockMode.Commit);
        whenNotLockedByOther(LockMode.Commit, LockMode.Commit, LockMode.Commit);
    }

    public void whenNotLockedByOther(LockMode initialLockMode, LockMode desiredLockMode, LockMode expectedLockMode) {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, initialLockMode.asInt());
        boolean result = ref.getLock().tryAcquire(tx, desiredLockMode);

        assertTrue(result);
        assertLockMode(ref, expectedLockMode);
        assertEquals(expectedLockMode.asInt(), tranlocal.getLockMode());
    }

    @Test
    public void whenNullTransaction() {
        GammaLongRef ref = new GammaLongRef(stm);

        try {
            ref.getLock().tryAcquire(null, LockMode.None);
            fail();
        } catch (NullPointerException expected) {
        }

        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenNullLockMode_thenTransactionAborted() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        try {
            ref.getLock().tryAcquire(tx, null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionPrepared() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        try {
            ref.getLock().tryAcquire(tx, LockMode.Read);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionAborted() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        try {
            ref.getLock().tryAcquire(tx, LockMode.Read);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionCommitted() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        try {
            ref.getLock().tryAcquire(tx, LockMode.Read);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }
}

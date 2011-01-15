package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public class AbstractGammaObject_hasReadConflictTest implements GammaConstants {
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenReadAndNoConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal read = ref.openForRead(tx, LOCKMODE_NONE);

        boolean hasReadConflict = ref.hasReadConflict(read);

        assertFalse(hasReadConflict);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenWriteAndNoConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal write = ref.openForWrite(tx, LOCKMODE_NONE);

        boolean hasReadConflict = ref.hasReadConflict(write);

        assertFalse(hasReadConflict);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenPrivatizedBySelf_thenNoConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal read = ref.openForRead(tx, LOCKMODE_COMMIT);

        boolean hasConflict = ref.hasReadConflict(read);

        assertFalse(hasConflict);
        assertSurplus(ref, 1);
        assertRefHasCommitLock(ref, tx);
    }

    @Test
    public void whenEnsuredBySelf_thenNoConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal read = ref.openForRead(tx, LOCKMODE_WRITE);

        boolean hasConflict = ref.hasReadConflict(read);

        assertFalse(hasConflict);
        assertSurplus(ref, 1);
        assertRefHasWriteLock(ref, tx);
    }

    @Test
    public void whenUpdatedByOther_thenConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal read = ref.openForRead(tx, LOCKMODE_NONE);

        //conflicting update
        ref.atomicIncrementAndGet(1);

        boolean hasConflict = ref.hasReadConflict(read);
        assertTrue(hasConflict);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    @Ignore
    public void whenFresh() {
        /*
        GammaTransaction tx = stm.startDefaultTransaction();
        GammaLongRef ref = new GammaLongRef(tx);
        GammaLongRefTranlocal tranlocal = tx.openForConstruction(ref);

        boolean conflict = ref.___hasReadConflict(tranlocal);

        assertFalse(conflict);
        assertSurplus(1, ref);
        assertRefHasCommitLock(ref, tx);*/
    }

    @Test
    public void whenValueChangedByOtherAndLockedForCommitByOther() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal read = ref.openForRead(tx, LOCKMODE_NONE);

        //do the conflicting update
        ref.atomicIncrementAndGet(1);

        //privatize it
        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        boolean hasConflict = ref.hasReadConflict(read);

        assertTrue(hasConflict);
        assertSurplus(ref, 1);
        assertRefHasCommitLock(ref,otherTx);
    }

    @Test
    public void whenValueChangedByOtherAndEnsuredAgain() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal read = ref.openForRead(tx, LOCKMODE_NONE);

        //do the conflicting update
        ref.atomicIncrementAndGet(1);

        //ensure it.
        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        boolean hasConflict = ref.hasReadConflict(read);

        assertTrue(hasConflict);
        assertSurplus(ref, 1);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void whenUpdateInProgressBecauseLockedByOther() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.openForRead(otherTx, LOCKMODE_COMMIT);

        boolean hasReadConflict = ref.hasReadConflict(tranlocal);

        assertTrue(hasReadConflict);
    }

    @Test
    public void whenAlsoReadByOther_thenNoConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        GammaTranlocal read = ref.openForRead(otherTx, LOCKMODE_NONE);

        boolean hasConflict = ref.hasReadConflict(read);

        assertFalse(hasConflict);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenPendingUpdateByOther_thenNoConflict() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.set(tx, 200);

        GammaTransaction otherTx = stm.startDefaultTransaction();
        GammaTranlocal read = ref.openForRead(otherTx, LOCKMODE_NONE);

        boolean hasConflict = ref.hasReadConflict(read);

        assertFalse(hasConflict);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    @Ignore
    public void whenReadBiased() {
    }
}

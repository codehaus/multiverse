package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.transactionalobjects.OrecTestUtils.assertSurplus;

public class LongTranlocal_prepareDirtyUpdatesTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void readonly_whenCheckConflictAndLockedByOther(){

    }

    @Test
    public void readonly_whenCheckConflictAndNoConflict() {
        readonly_whenCheckConflictAndNoConflict(LOCKMODE_NONE, LOCKMODE_UPDATE);
        readonly_whenCheckConflictAndNoConflict(LOCKMODE_UPDATE, LOCKMODE_UPDATE);
        readonly_whenCheckConflictAndNoConflict(LOCKMODE_COMMIT, LOCKMODE_COMMIT);
    }

    public void readonly_whenCheckConflictAndNoConflict(int lockMode, int resultingLockMode) {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, lockMode);
        ref.ensure(tx);
        boolean success = tranlocal.prepareDirtyUpdates(pool, tx, 1);

        assertTrue(success);
        assertHasDepartObligation(tranlocal, true);
        assertRefHasLockMode(ref, tx, resultingLockMode);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void readonly_notLockedByOther() {
        readonly_notLockedByOther(false, LOCKMODE_NONE);
        readonly_notLockedByOther(false, LOCKMODE_UPDATE);
        readonly_notLockedByOther(false, LOCKMODE_COMMIT);
        readonly_notLockedByOther(true, LOCKMODE_NONE);
        readonly_notLockedByOther(true, LOCKMODE_UPDATE);
        readonly_notLockedByOther(true, LOCKMODE_COMMIT);
    }

    public void readonly_notLockedByOther(boolean readBiased, int lockMode) {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue, readBiased);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, lockMode);
        boolean success = tranlocal.prepareDirtyUpdates(pool, tx, 1);

        assertTrue(success);
        assertHasDepartObligation(tranlocal, !readBiased);
        assertRefHasLockMode(ref, tx, lockMode);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void readonly_lockedByOther() {
        readonly_lockedByOther(false, LOCKMODE_UPDATE);
        readonly_lockedByOther(false, LOCKMODE_COMMIT);
        readonly_lockedByOther(true, LOCKMODE_UPDATE);
        readonly_lockedByOther(true, LOCKMODE_COMMIT);
    }

    public void readonly_lockedByOther(boolean readBiased, int otherLockMode) {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue, readBiased);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, otherLockMode);

        tx.openForRead(ref, otherLockMode);

        boolean success = tranlocal.prepareDirtyUpdates(pool, tx, 1);

        assertTrue(success);
        assertHasDepartObligation(tranlocal, !readBiased);
        assertRefHasLockMode(ref, tx, otherLockMode);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void dirtyUpdate_andConflict() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value++;

        ref.atomicIncrementAndGet(1);

        boolean success = tranlocal.prepareDirtyUpdates(pool, tx, 1);
        assertFalse(success);

        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void update_lockedByOtherAndDirty_thenConflict() {
        update_lockedByOtherAndDirty_thenConflict(false, LOCKMODE_UPDATE);
        update_lockedByOtherAndDirty_thenConflict(false, LOCKMODE_COMMIT);
        update_lockedByOtherAndDirty_thenConflict(true, LOCKMODE_UPDATE);
        update_lockedByOtherAndDirty_thenConflict(true, LOCKMODE_COMMIT);
    }

    public void update_lockedByOtherAndDirty_thenConflict(boolean readBiased, int otherLockMode) {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue, readBiased);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, otherLockMode);

        boolean success = tranlocal.prepareDirtyUpdates(pool, tx, 1);

        assertFalse(success);
        assertHasDepartObligation(tranlocal, !readBiased);
        assertRefHasLockMode(ref, otherTx, otherLockMode);
        if (readBiased) {
            assertSurplus(1, ref);
        } else {
            assertSurplus(2, ref);
        }
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void update_lockedByOtherAndNotDirty_thenNoConflict() {
        update_lockedByOtherAndNotDirty_thenNoConflict(false, LOCKMODE_UPDATE);
        update_lockedByOtherAndNotDirty_thenNoConflict(false, LOCKMODE_COMMIT);
        update_lockedByOtherAndNotDirty_thenNoConflict(true, LOCKMODE_UPDATE);
        update_lockedByOtherAndNotDirty_thenNoConflict(true, LOCKMODE_COMMIT);
    }

    public void update_lockedByOtherAndNotDirty_thenNoConflict(boolean readBiased, int otherLockMode) {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue, readBiased);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, otherLockMode);

        boolean success = tranlocal.prepareDirtyUpdates(pool, tx, 1);

        assertTrue(success);
        assertHasDepartObligation(tranlocal, !readBiased);
        assertRefHasLockMode(ref, otherTx, otherLockMode);
        if (readBiased) {
            assertSurplus(1, ref);
        } else {
            assertSurplus(2, ref);
        }
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void construction_whenNoChange() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal tranlocal = tx.openForConstruction(ref);

        boolean success = tranlocal.prepareDirtyUpdates(pool, tx, 1);

        assertTrue(success);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertHasNoDepartObligation(tranlocal);
        assertVersionAndValue(ref, BetaTransactionalObject.VERSION_UNCOMMITTED, 0);
    }

    @Test
    public void construction_whenChange() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        BetaLongRefTranlocal tranlocal = tx.openForConstruction(ref);
        tranlocal.value++;

        boolean success = tranlocal.prepareDirtyUpdates(pool, tx, 1);

        assertTrue(success);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertHasNoDepartObligation(tranlocal);
        assertVersionAndValue(ref, BetaTransactionalObject.VERSION_UNCOMMITTED, 0);
    }
}

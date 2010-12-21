package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;

public class LongTranlocal_prepareAllUpdatesTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void readonly_whenCheckConflictAndNoConflict(){
        readonly_whenCheckConflictAndNoConflict(LOCKMODE_NONE, LOCKMODE_UPDATE);
        readonly_whenCheckConflictAndNoConflict(LOCKMODE_UPDATE, LOCKMODE_UPDATE);
        readonly_whenCheckConflictAndNoConflict(LOCKMODE_COMMIT, LOCKMODE_COMMIT);
    }

    public void readonly_whenCheckConflictAndNoConflict(int lockMode, int resultingLockMode){
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, lockMode);
        ref.ensure(tx);
        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);

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
        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);

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

        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);

        assertTrue(success);
        assertHasDepartObligation(tranlocal, !readBiased);
        assertRefHasLockMode(ref, tx, otherLockMode);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    // ---------------------------------------------------------

    @Test
    public void nonDirtyUpdate_andConflict() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);
        assertFalse(success);

        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void update_notLockedByOther() {
        update_notLockedByOther(true, true, LOCKMODE_NONE);
        update_notLockedByOther(true, true, LOCKMODE_UPDATE);
        update_notLockedByOther(true, true, LOCKMODE_COMMIT);
        update_notLockedByOther(true, false, LOCKMODE_NONE);
        update_notLockedByOther(true, false, LOCKMODE_UPDATE);
        update_notLockedByOther(true, false, LOCKMODE_COMMIT);
        update_notLockedByOther(false, true, LOCKMODE_NONE);
        update_notLockedByOther(false, true, LOCKMODE_UPDATE);
        update_notLockedByOther(false, true, LOCKMODE_COMMIT);
        update_notLockedByOther(false, false, LOCKMODE_NONE);
        update_notLockedByOther(false, false, LOCKMODE_UPDATE);
        update_notLockedByOther(false, false, LOCKMODE_COMMIT);
    }

    public void update_notLockedByOther(boolean readBiased, boolean dirty, int lockMode) {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue, readBiased);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, lockMode);
        tranlocal.value = dirty ? tranlocal.value + 1 : tranlocal.value;

        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);
        assertTrue(success);

        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertHasDepartObligation(tranlocal, !readBiased);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    // ---------------------------------------------------------

    @Test
    public void update_whenConflict() {
        update_whenConflict(true, true);
        update_whenConflict(true, false);
        update_whenConflict(false, true);
        update_whenConflict(false, false);
    }

    public void update_whenConflict(boolean readBiased, boolean dirty) {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue, readBiased);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value = dirty ? tranlocal.value + 1 : tranlocal.value;

        ref.atomicIncrementAndGet(1);

        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);

        assertFalse(success);
        assertRefHasNoLocks(ref);
        assertHasDepartObligation(tranlocal, !readBiased);
        if (readBiased) {
            assertSurplus(0, ref);
        } else {
            assertSurplus(1, ref);
        }
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void update_andLockedByOther_thenConflict() {
        update_lockedByOther_thenConflict(false, false, LOCKMODE_UPDATE);
        update_lockedByOther_thenConflict(false, false, LOCKMODE_COMMIT);
        update_lockedByOther_thenConflict(false, true, LOCKMODE_UPDATE);
        update_lockedByOther_thenConflict(false, true, LOCKMODE_COMMIT);
        update_lockedByOther_thenConflict(true, false, LOCKMODE_UPDATE);
        update_lockedByOther_thenConflict(true, false, LOCKMODE_COMMIT);
        update_lockedByOther_thenConflict(true, true, LOCKMODE_UPDATE);
        update_lockedByOther_thenConflict(true, true, LOCKMODE_COMMIT);
    }

    public void update_lockedByOther_thenConflict(boolean readBiased, boolean dirty, int otherLockMode) {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue, readBiased);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tranlocal.value = dirty ? tranlocal.value + 1 : tranlocal.value;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, otherLockMode);

        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);

        assertHasDepartObligation(tranlocal, !readBiased);
        assertFalse(success);
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

        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);

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

        boolean success = tranlocal.prepareAllUpdates(pool, tx, 1);

        assertTrue(success);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertHasNoDepartObligation(tranlocal);
        assertVersionAndValue(ref, BetaTransactionalObject.VERSION_UNCOMMITTED, 0);
    }
}

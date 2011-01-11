package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;

public class LongTranlocal_openForReadTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void selfUpgrade_whenNoLockAndUpgradeToNone() {
        self_upgrade(LOCKMODE_NONE, LOCKMODE_NONE, LOCKMODE_NONE);
    }

    @Test
    public void selfUpgrade_whenNoLockAndUpgradeToEnsure() {
        self_upgrade(LOCKMODE_NONE, LOCKMODE_WRITE, LOCKMODE_WRITE);
    }

    @Test
    public void selfUpgrade_whenNoLockAndUpgradeToPrivatize() {
        self_upgrade(LOCKMODE_NONE, LOCKMODE_COMMIT, LOCKMODE_COMMIT);
    }

    @Test
    public void selfUpgrade_whenEnsuredAndUpgradeToNone() {
        self_upgrade(LOCKMODE_WRITE, LOCKMODE_NONE, LOCKMODE_WRITE);
    }

    @Test
    public void selfUpgrade_whenEnsuredAndUpgradeToEnsure() {
        self_upgrade(LOCKMODE_WRITE, LOCKMODE_WRITE, LOCKMODE_WRITE);
    }

    @Test
    public void selfUpgrade_whenEnsuredAndUpgradeToPrivatize() {
        self_upgrade(LOCKMODE_WRITE, LOCKMODE_COMMIT, LOCKMODE_COMMIT);
    }

    @Test
    public void selfUpgrade_whenPrivatizedAndUpgradeToNone() {
        self_upgrade(LOCKMODE_COMMIT, LOCKMODE_NONE, LOCKMODE_COMMIT);
    }

    @Test
    public void selfUpgrade_whenPrivatizeAndUpgradeToEnsure() {
        self_upgrade(LOCKMODE_COMMIT, LOCKMODE_WRITE, LOCKMODE_COMMIT);
    }

    @Test
    public void selfUpgrade_whenNoPrivatizeAndUpgradeToPrivatize() {
        self_upgrade(LOCKMODE_COMMIT, LOCKMODE_COMMIT, LOCKMODE_COMMIT);
    }

    public void self_upgrade(int firstTimeLockMode, int secondTimeLockMode, int expected) {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref,firstTimeLockMode);

        tranlocal.openForRead(secondTimeLockMode);

        assertHasVersionAndValue(tranlocal, initialVersion, initialValue);
        assertIsActive(tx);
        assertRefHasLockMode(ref, tx, expected);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void locking_whenEnsuredByOther_thenSuccess() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        assertHasVersionAndValue(tranlocal, initialVersion, initialValue);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void locking_whenPrivatizedByOther_thenReadWriteConflict() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        BetaTransaction tx = stm.startDefaultTransaction();

        try {
            tx.openForRead(ref,LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void locking_whenEnsuredBySelf() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();

        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_WRITE);

        assertHasVersionAndValue(tranlocal, initialVersion, initialValue);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void locking_whenPrivatizedBySelf() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref,LOCKMODE_COMMIT);


        assertHasVersionAndValue(tranlocal, initialVersion, initialValue);
        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNotOpenedBefore() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);

        assertRefHasNoLocks(ref);
        assertTrue(tranlocal.isReadonly());
        assertHasVersionAndValue(tranlocal, initialVersion, initialValue);
        assertEquals(initialValue, tranlocal.oldValue);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref,LOCKMODE_NONE);
        tranlocal.openForRead(LOCKMODE_NONE);

        assertRefHasNoLocks(ref);
        assertTrue(tranlocal.isReadonly());
        assertHasVersionAndValue(tranlocal, initialVersion, initialValue);
        assertEquals(initialValue, tranlocal.oldValue);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        long initialValue = 10;
        BetaLongRef ref = new BetaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForRead(ref, LOCKMODE_NONE);
        tranlocal.openForWrite(LOCKMODE_NONE);
        tranlocal.openForRead(LOCKMODE_NONE);

        assertRefHasNoLocks(ref);
        assertFalse(tranlocal.isReadonly());
        assertHasVersionAndValue(tranlocal, initialVersion, initialValue);
        assertEquals(initialValue, tranlocal.oldValue);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForConstruction() {
        /*
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);

        long initialVersion = ref.getVersion();


        BetaLongRefTranlocal tranlocal = tx.open(ref);
        tranlocal.openForConstruction();
        tranlocal.openForRead(LOCKMODE_NONE);

        assertFalse(tranlocal.isCommitted);
        assertEquals(initialVersion, tranlocal.version);
        assertEquals(initialValue, tranlocal.value);
        assertEquals(initialValue, tranlocal.oldValue);
        assertVersionAndValue(ref, initialVersion, initialValue);
        */
    }

    @Test
    @Ignore
    public void state_whenTransactionAborted() {

    }

    @Test
    @Ignore
    public void state_whenTransactionPrepared() {

    }

    @Test
    @Ignore
    public void state_whenTransactionCommitted() {

    }

}

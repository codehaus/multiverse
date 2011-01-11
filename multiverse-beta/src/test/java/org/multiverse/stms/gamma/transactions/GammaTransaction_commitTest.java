package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class GammaTransaction_commitTest<T extends GammaTransaction> implements GammaConstants {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    protected abstract void assertCleaned(T transaction);

    @Test
    public void whenUnused() {
        GammaTransaction tx = newTransaction();

        tx.commit();

        assertEquals(TransactionStatus.Committed, tx.getStatus());
    }

    @Test
    public void whenConflict() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        //a conflicting write.
        T otherTx = newTransaction();
        ref.openForWrite(otherTx, LOCKMODE_NONE).long_value++;
        otherTx.commit();

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertEquals(initialValue + 1, ref.value);
        assertEquals(initialVersion + 1, ref.version);
        assertCleaned(tx);
    }

    //todo: dirty checking
    //todo: lock releasing

    @Test
    public void whenContainsDirtyWrite() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue + 1, ref.value);
        assertEquals(initialVersion + 1, ref.version);
        assertCleaned(tx);
    }

    @Test
    public void whenMultipleCommitsUsingNewTransaction() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        int txCount = 10;
        for (int k = 0; k < txCount; k++) {
            T tx = newTransaction();
            ref.openForWrite(tx, LOCKMODE_NONE).long_value++;
            tx.commit();
        }

        assertEquals(initialValue + txCount, ref.value);
        assertEquals(initialVersion + txCount, ref.version);
    }

    @Test
    public void whenMultipleCommitsUsingSame() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        int txCount = 10;
        T tx = newTransaction();
        for (int k = 0; k < txCount; k++) {
            ref.openForWrite(tx, LOCKMODE_NONE).long_value++;
            tx.commit();
            tx.hardReset();
        }

        assertEquals(initialValue + txCount, ref.value);
        assertEquals(initialVersion + txCount, ref.version);
    }

    @Test
    public void whenPreparedAndContainsRead() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);
        tx.prepare();

        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.version);
        assertCleaned(tx);
    }

    @Test
    public void whenPreparedAndContainsWrite() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;
        tx.prepare();

        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue + 1, ref.value);
        assertEquals(initialVersion + 1, ref.version);
        assertCleaned(tx);
    }

    // ================================ dirty check ================================

    @Test
    public void whenContainsRead() {
        whenContainsRead(true,LockMode.None);
        whenContainsRead(true, LockMode.Read);
        whenContainsRead(true, LockMode.Write);
        whenContainsRead(true, LockMode.Commit);

        whenContainsRead(false,LockMode.None);
        whenContainsRead(false, LockMode.Read);
        whenContainsRead(false, LockMode.Write);
        whenContainsRead(false, LockMode.Commit);
    }

    public void whenContainsRead(boolean prepareFirst, LockMode readLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        ref.openForRead(tx, readLockMode.asInt());
        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertIsCommitted(tx);
        assertLockMode(ref, LockMode.None);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertCleaned(tx);
    }

    // ================================ dirty check ================================

    @Test
    public void dirty_whenNoDirtyCheckAndNoDirtyWrite() {
        dirty_whenNoDirtyCheckAndNoDirtyWrite(true);
        dirty_whenNoDirtyCheckAndNoDirtyWrite(false);
    }

    public void dirty_whenNoDirtyCheckAndNoDirtyWrite(boolean prepareFirst) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.dirtyCheck = false;
        T tx = newTransaction(config);
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);

        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion + 1, ref.version);
        assertCleaned(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void dirty_whenNoDirtyCheckAndDirtyWrite() {
        dirty_whenNoDirtyCheckAndDirtyWrite(true);
        dirty_whenNoDirtyCheckAndDirtyWrite(false);
    }

    public void dirty_whenNoDirtyCheckAndDirtyWrite(boolean prepareFirst) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.dirtyCheck = false;
        T tx = newTransaction(config);
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue + 1, ref.value);
        assertEquals(initialVersion + 1, ref.version);
        assertCleaned(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void dirty_whenDirtyCheckAndNoDirtyWrite() {
        dirty_whenDirtyCheckAndNoDirtyWrite(true);
        dirty_whenDirtyCheckAndNoDirtyWrite(false);
    }

    public void dirty_whenDirtyCheckAndNoDirtyWrite(boolean prepareFirst) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.dirtyCheck = true;
        T tx = newTransaction(config);
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue, ref.value);
        assertEquals(initialVersion, ref.version);
        assertCleaned(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void dirty_whenDirtyCheckAndDirtyWrite() {
        dirty_whenDirtyCheckAndDirtyWrite(true);
        dirty_whenDirtyCheckAndDirtyWrite(false);
    }

    public void dirty_whenDirtyCheckAndDirtyWrite(boolean prepareFirst) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);
        config.dirtyCheck = true;
        T tx = newTransaction(config);
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;
        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue + 1, ref.value);
        assertEquals(initialVersion + 1, ref.version);
        assertCleaned(tx);
        assertRefHasNoLocks(ref);
    }


    // =============================== locked by other =============================

    @Test
    public void conflict_dirty_whenReadLockedByOther() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        T otherTx = newTransaction();
        ref.openForRead(otherTx, LOCKMODE_READ);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasReadLock(ref, otherTx);
        assertReadLockCount(ref, 1);
    }

    @Test
    public void conflict_dirty_whenWriteLockedByOther() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        T otherTx = newTransaction();
        ref.openForRead(otherTx, LOCKMODE_WRITE);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasWriteLock(ref, otherTx);
    }

    @Test
    public void conflict_dirty_whenCommitLockedByOther() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        T otherTx = newTransaction();
        ref.openForRead(otherTx, LOCKMODE_COMMIT);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasCommitLock(ref, otherTx);
    }

    // ========================= states ==================================

    @Test
    public void whenPreparedAndUnused() {
        T tx = newTransaction();
        tx.prepare();

        tx.commit();
        assertIsCommitted(tx);
        assertCleaned(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.abort();

        try {
            tx.commit();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertCleaned(tx);
    }

    @Test
    public void whenCommitted_thenIgnored() {
        T tx = newTransaction();
        tx.commit();

        tx.commit();

        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertCleaned(tx);
    }
}

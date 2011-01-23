package org.multiverse.stms.gamma.transactions.fat;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.SomeError;
import org.multiverse.SomeUncheckedException;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.AbortOnlyException;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.*;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionConfiguration;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class FatGammaTransaction_commitTest<T extends GammaTransaction> implements GammaConstants {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    protected abstract void assertCleaned(T transaction);

    @Test
    public void whenContainsConstructedIntRef() {
        T tx = newTransaction();
        int initialValue = 10;
        GammaIntRef ref = new GammaIntRef(tx, initialValue);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, GammaObject.VERSION_UNCOMMITTED + 1, initialValue);
        assertSurplus(ref, 0);
        assertReadonlyCount(ref, 0);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenContainsConstructedBooleanRef() {
        T tx = newTransaction();
        boolean initialValue = true;
        GammaBooleanRef ref = new GammaBooleanRef(tx, initialValue);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, GammaObject.VERSION_UNCOMMITTED + 1, initialValue);
        assertSurplus(ref, 0);
        assertReadonlyCount(ref, 0);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenContainsConstructedDoubleRef() {
        T tx = newTransaction();
        double initialValue = 10;
        GammaDoubleRef ref = new GammaDoubleRef(tx, initialValue);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, GammaObject.VERSION_UNCOMMITTED + 1, initialValue);
        assertSurplus(ref, 0);
        assertReadonlyCount(ref, 0);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenContainsConstructedRef() {
        T tx = newTransaction();
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(tx, initialValue);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, GammaObject.VERSION_UNCOMMITTED + 1, initialValue);
        assertSurplus(ref, 0);
        assertReadonlyCount(ref, 0);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenContainsConstructedLongRef() {
        T tx = newTransaction();
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(tx, initialValue);
        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, GammaObject.VERSION_UNCOMMITTED + 1, initialValue);
        assertSurplus(ref, 0);
        assertReadonlyCount(ref, 0);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenCommuteThrowsRuntimeException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        LongFunction function = mock(LongFunction.class);
        when(function.call(initialValue)).thenThrow(new SomeUncheckedException());
        ref.commute(tx, function);

        try {
            tx.commit();
            fail();
        } catch (SomeUncheckedException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenCommuteThrowsError() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        LongFunction function = mock(LongFunction.class);
        when(function.call(initialValue)).thenThrow(new SomeError());
        ref.commute(tx, function);

        try {
            tx.commit();
            fail();
        } catch (SomeError expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenContainsCommute() {
        long initialValue = 10;

        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        ref.commute(tx, Functions.newIncLongFunction());
        tx.commit();

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void abortOnly_whenUnused() {
        T tx = newTransaction();
        tx.setAbortOnly();

        try {
            tx.commit();
            fail();
        } catch (AbortOnlyException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void abortOnly_whenDirty() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        tx.setAbortOnly();

        try {
            ref.set(tx, initialValue + 1);
            tx.commit();
            fail();
        } catch (AbortOnlyException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenBooleanRef() {
        boolean initialValue = false;
        GammaBooleanRef ref = new GammaBooleanRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        ref.set(tx, true);
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, true);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenIntRef() {
        int initialValue = 10;
        GammaIntRef ref = new GammaIntRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        ref.set(tx, initialValue + 1);
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenLongRef() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        ref.set(tx, initialValue + 1);
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenDoubleRef() {
        double initialValue = 10;
        GammaDoubleRef ref = new GammaDoubleRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        ref.set(tx, initialValue + 1);
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenRef() {
        String initialValue = "foo";
        GammaRef<String> ref = new GammaRef<String>(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        String newValue = "bar";
        ref.set(tx, newValue);
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, "bar");
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenContainsListener() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction listeningTx = newTransaction();
        ref.openForRead(listeningTx, LOCKMODE_NONE);

        try {
            listeningTx.retry();
            fail();
        } catch (Retry retry) {
        }

        GammaTransaction tx = newTransaction();
        ref.openForWrite(tx, LOCKMODE_NONE).long_value++;
        tx.commit();

        assertTrue(listeningTx.listener.isOpen());
        assertNull(getField(ref, "listeners"));
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

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
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
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
        assertEquals(initialValue + 1, ref.long_value);
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
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue + 1, ref.long_value);
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

        assertEquals(initialValue + txCount, ref.long_value);
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

        assertEquals(initialValue + txCount, ref.long_value);
        assertEquals(initialVersion + txCount, ref.version);
    }

    @Test
    public void whenPreparedAndContainsRead() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);
        tx.prepare();

        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue, ref.long_value);
        assertEquals(initialVersion, ref.version);
        assertCleaned(tx);
    }

    @Test
    public void whenPreparedAndContainsWrite() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;
        tx.prepare();

        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue + 1, ref.long_value);
        assertEquals(initialVersion + 1, ref.version);
        assertCleaned(tx);
    }

    // ================================ dirty check ================================

    @Test
    public void whenContainsRead() {
        whenContainsRead(true, LockMode.None);
        whenContainsRead(true, LockMode.Read);
        whenContainsRead(true, LockMode.Write);
        whenContainsRead(true, LockMode.Exclusive);

        whenContainsRead(false, LockMode.None);
        whenContainsRead(false, LockMode.Read);
        whenContainsRead(false, LockMode.Write);
        whenContainsRead(false, LockMode.Exclusive);
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
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);

        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue, ref.long_value);
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
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue + 1, ref.long_value);
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
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue, ref.long_value);
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
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;
        if (prepareFirst) {
            tx.prepare();
        }
        tx.commit();

        assertNull(tranlocal.owner);
        assertEquals(TransactionStatus.Committed, tx.getStatus());
        assertEquals(initialValue + 1, ref.long_value);
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
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
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
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
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
    public void conflict_dirty_whenExclusiveLockedByOther() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;

        T otherTx = newTransaction();
        ref.openForRead(otherTx, LOCKMODE_EXCLUSIVE);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertRefHasExclusiveLock(ref, otherTx);
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

package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class GammaTransaction_prepareTest<T extends GammaTransaction> implements GammaConstants {

    public GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    @Test
    public void whenContainsNormalRead() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, LOCKMODE_NONE);
        tx.prepare();

        assertIsPrepared(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    @Ignore
    public void whenAbortOnly(){

    }

    @Test
     @Ignore
     public void whenContainsReadLockedRead() {

     }

    @Test
    @Ignore
    public void whenContainsWriteLockedRead() {

    }

    @Test
    @Ignore
    public void whenContainsCommitLockedRead() {

    }

    @Test
    @Ignore
    public void writeSkew(){

    }

    @Test
    @Ignore
    public void whenContainsCommute(){

    }

    @Test
    @Ignore
    public void whenContainsCommuteThatConflicts(){

    }

    @Test
    @Ignore
    public void whenContainsConstructed(){}

    @Test
    public void whenContainsDirtyWrite() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        T tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, LOCKMODE_NONE);
        tranlocal.long_value++;
        tx.prepare();

        assertIsPrepared(tx);
        assertRefHasCommitLock(ref, tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    @Ignore
    public void whenContainsDirtyAndWriteLockedWrite() {

    }

    @Test
    @Ignore
    public void whenContainsDirtyAndCommitLockedWrite() {

    }

    @Test
    @Ignore
    public void whenContainsDirtyAndReadLockedWrite() {

    }

    @Test
    @Ignore
    public void whenPreparedAndContainsDirtyWrite() {

    }

    // =============================== dirty check =================================

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
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion,initialValue);
        assertRefHasReadLock(ref,otherTx);
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
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion,initialValue);
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
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion,initialValue);
        assertRefHasCommitLock(ref, otherTx);
    }

    // ================================ states =====================================

    @Test
    public void whenPreparedAndUnused() {
        T tx = newTransaction();
        tx.prepare();

        tx.prepare();

        assertIsPrepared(tx);
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.abort();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        T tx = newTransaction();
        tx.commit();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}

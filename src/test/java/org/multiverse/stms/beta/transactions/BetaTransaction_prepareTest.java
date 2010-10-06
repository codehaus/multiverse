package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.IsolationLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.functions.Functions;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.functions.Functions.newIncLongFunction;
import static org.multiverse.stms.beta.BetaStmUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public abstract class BetaTransaction_prepareTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    public abstract boolean doesTransactionSupportCommute();

    public abstract int getTransactionMaxCapacity();

    public abstract boolean isSupportingWriteSkewDetection();

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenUnused() {
        BetaTransaction tx = newTransaction();

        tx.prepare();

        assertIsPrepared(tx);
    }

    @Test
    public void whenReadIsConflictedByWrite() {
        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        tx.prepare();

        assertIsPrepared(tx);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, version, 11);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadIsConflictedByLock_thenPrepareSuccess() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        tx.prepare();

        assertIsPrepared(tx);
        assertIsActive(otherTx);
        assertHasCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 10);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenUpdate() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.prepare();

        assertIsPrepared(tx);

        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 10);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenContainsPrivatizedNoneDirtyWrite() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);
        tx.prepare();

        assertIsPrepared(tx);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 10);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenContainsEnsuredDirtyWrite_thenLockUpgradedToPrivatized() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_UPDATE);
        write.value++;
        tx.prepare();

        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertTrue(write.isLockOwner);
        assertFalse(write.isCommuting);
        assertNull(write.headCallable);
        assertTrue(write.hasDepartObligation);
        assertTrue(write.isDirty);
        assertEquals(10, write.oldValue);
        assertEquals(11, write.value);
        assertEquals(version, write.version);
        
        assertIsPrepared(tx);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 10);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    @Ignore
    public void whenContainsEnsuredNonDirtyWriteBySelf_thenLockUpgradedToPrivatized() {

    }


    @Test
    public void whenDirtyUpdateAndPrivatizedByOther_thenWriteConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        try {
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertIsActive(otherTx);

        assertHasCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 10);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    @Ignore
    public void whenNonDirtyUpdateAndEnsuredByOther_then(){

    }

    @Test
    public void whenDirtyUpdateAndEnsuredByOther_thenWriteConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_UPDATE);

        try {
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertIsActive(otherTx);

        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 10);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenOnlyConstructed() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        tx.prepare();

        assertFalse(constructed.isCommitted);
        assertFalse(constructed.hasDepartObligation);
        assertIsPrepared(tx);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, 0, 0);
    }

    @Test
    public void whenConflictingWrite() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;

        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();
        try {
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, version, 1);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenMultipleItems() {
        assumeTrue(getTransactionMaxCapacity() >= 3);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);
        write1.value++;
        LongRefTranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);
        write2.value++;
        LongRefTranlocal write3 = tx.openForWrite(ref3, LOCKMODE_NONE);
        write3.value++;
        tx.prepare();

        assertSame(tx, ref1.___getLockOwner());
        assertSame(tx, ref2.___getLockOwner());
        assertSame(tx, ref3.___getLockOwner());
        assertHasCommitLock(ref1);
        assertHasCommitLock(ref2);
        assertHasCommitLock(ref3);
        assertSurplus(1, ref1);
        assertSurplus(1, ref2);
        assertSurplus(1, ref3);
        assertUpdateBiased(ref1);
        assertUpdateBiased(ref2);
        assertUpdateBiased(ref3);
    }

    @Test
    @Ignore
    public void whenOneOfTheItemsFails() {

    }

    @Test
    public void whenAbortOnly() {
        BetaTransaction tx = newTransaction();
        tx.setAbortOnly();

        try {
            tx.prepare();
            fail();
        } catch (ReadWriteConflict conflict) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void commute_whenReferenceHasMultipleCommutes() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, Functions.newIncLongFunction(1));
        tx.commute(ref, Functions.newIncLongFunction(1));
        tx.commute(ref, Functions.newIncLongFunction(1));
        tx.prepare();

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertNotNull(tranlocal);
        assertEquals(3, tranlocal.value);
        assertTrue(tranlocal.isDirty);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertTrue(tranlocal.hasDepartObligation);
        assertSame(ref, tranlocal.owner);

        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void commute_whenMultipleReferencesHaveCommute() {
        assumeTrue(doesTransactionSupportCommute());
        assumeTrue(getTransactionMaxCapacity() >= 3);

        BetaLongRef ref1 = newLongRef(stm, 10);
        BetaLongRef ref2 = newLongRef(stm, 20);
        BetaLongRef ref3 = newLongRef(stm, 30);

        BetaTransaction tx = newTransaction();
        tx.commute(ref1, Functions.newIncLongFunction(1));
        tx.commute(ref2, Functions.newIncLongFunction(1));
        tx.commute(ref3, Functions.newIncLongFunction(1));
        tx.prepare();

        LongRefTranlocal commute1 = (LongRefTranlocal) tx.get(ref1);
        LongRefTranlocal commute2 = (LongRefTranlocal) tx.get(ref2);
        LongRefTranlocal commute3 = (LongRefTranlocal) tx.get(ref3);

        assertNotNull(commute1);
        assertEquals(11, commute1.value);
        assertNotNull(commute1);
        assertEquals(21, commute2.value);
        assertNotNull(commute1);
        assertEquals(31, commute3.value);
    }

    @Test
    public void commute_whenHasCommuteAndNoDirtyCheck() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(false);
        BetaTransaction tx = newTransaction(config);
        tx.commute(ref, newIncLongFunction(1));
        tx.prepare();

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertNotNull(tranlocal);
        assertEquals(1, tranlocal.value);
        assertEquals(0, tranlocal.oldValue);
        assertEquals(initialVersion, tranlocal.version);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertTrue(tranlocal.isDirty);
        assertTrue(tranlocal.isLockOwner);
        assertSame(ref, tranlocal.owner);

        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void commute_whenHasCommuteAndDirtyCheck() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setDirtyCheckEnabled(true);
        BetaTransaction tx = newTransaction(config);
        tx.commute(ref, newIncLongFunction(1));
        tx.prepare();

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertNotNull(tranlocal);
        assertEquals(1, tranlocal.value);
        assertEquals(0, tranlocal.oldValue);
        assertEquals(initialVersion, tranlocal.version);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertTrue(tranlocal.isLockOwner);
        assertSame(ref, tranlocal.owner);

        assertEquals(1, tranlocal.value);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void commute_whenHasCommuteButLockedByOtherTransaction_thenWriteConflict() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = newTransaction();
        tx.commute(ref, newIncLongFunction(1));

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        try {
            tx.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);

        assertHasCommitLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, 0);
    }

    @Test
    public void commute_whenHasCommuteThatConflicts() {
        assumeTrue(doesTransactionSupportCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, newIncLongFunction(1));

        //conflicting write
        ref.atomicIncrementAndGet(1);
        long version = ref.getVersion();

        tx.prepare();

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertNotNull(tranlocal);
        assertEquals(2, tranlocal.value);
        assertEquals(1, tranlocal.oldValue);
        assertEquals(version, tranlocal.version);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertTrue(tranlocal.hasDepartObligation);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isDirty);

        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenWriteSkewStillPossibleWithWriteSkewEnabled() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx1 = newTransaction();
        tx1.openForWrite(ref1, LOCKMODE_NONE).value++;
        tx1.openForRead(ref2, LOCKMODE_NONE);

        BetaTransaction tx2 = newTransaction();
        tx2.openForRead(ref1, LOCKMODE_NONE);
        tx2.openForWrite(ref2, LOCKMODE_NONE).value++;

        tx1.prepare();
        tx2.prepare();
    }

    @Test
    @Ignore
    public void writeSkew(){

    }

    @Test
    public void whenSerializedIsolationLevel_thenWriteSkewDetectedAndReadWriteConflictThrown() {
        assumeTrue(getTransactionMaxCapacity() >= 2);
        assumeTrue(isSupportingWriteSkewDetection());

        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx1 = newTransaction();
        tx1.openForWrite(ref1, LOCKMODE_NONE).value++;
        tx1.openForRead(ref2, LOCKMODE_NONE);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setIsolationLevel(IsolationLevel.Serializable);
        BetaTransaction tx2 = newTransaction(config);
        tx2.openForRead(ref1, LOCKMODE_NONE);
        tx2.openForWrite(ref2, LOCKMODE_NONE).value++;

        tx1.prepare();

        try {
            tx2.prepare();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx2);
    }

    @Test
    public void whenAlreadyPrepared_thenNoProblems() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);
        write.value++;
        tx.prepare();

        tx.prepare();
        assertIsPrepared(tx);

        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertVersionAndValue(ref, version, 0);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertFalse(write.isCommitted);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.abort();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}

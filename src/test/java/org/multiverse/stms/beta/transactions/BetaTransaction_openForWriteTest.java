package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaTransactionalObject;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public abstract class BetaTransaction_openForWriteTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    public abstract boolean isSupportingCommute();

    public abstract int getTransactionMaxCapacity();

    public abstract boolean hasLocalConflictCounter();

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void test() {
        BetaTransaction tx = newTransaction();
        assertNotNull(tx);
    }

    @Test
    public void whenInitialOpenForWrite() {
        BetaLongRef ref = newLongRef(stm, 0);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        Tranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertFalse(write.isCommitted());
        assertSame(committed, write.read);
        assertSame(write.owner, ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForReadThenNoReadConflictEvenIfUpdatedByOther() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = newTransaction();
        Tranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        Tranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertNotNull(write);
        assertFalse(write.isCommitted());
        assertSame(read, write.read);
        assertSame(ref, write.owner);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForWriteThenNoReadConflictEvenIfUpdatedByOther() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransaction tx = newTransaction();
        Tranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);

        ref.atomicIncrementAndGet(1);

        Tranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(write2, write1);
        assertFalse(write2.isCommitted());
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    //todo
    @Test
    public void whenConstructedAndLock() {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;

        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(constructed, write);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasNoUpdates(tx);
    }

    //todo
    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);

        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(config);
        LongRefTranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        LongRefTranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());

        assertIsActive(tx);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
    }
   
    @Test
    public void conflictCounterIsOnlySetOnFirstRead() {
        assumeTrue(getTransactionMaxCapacity()>=2);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        stm.getGlobalConflictCounter().signalConflict(ref1);

        tx.openForWrite(ref1, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());

        tx.openForWrite(ref2, LOCKMODE_NONE);

        assertEquals(tx.getLocalConflictCounter().get(), stm.getGlobalConflictCounter().count());
    }

    @Test
    public void whenNull_thenNullPointerException() {
        BetaTransaction tx = newTransaction();

        try {
            tx.openForWrite((BetaLongRef) null, LOCKMODE_NONE);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenReadonly_thenAbortedAndReadonlyException() {
        BetaTransactionalObject ref = newLongRef(stm, 0);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).setReadonly(true);
        BetaTransaction tx = newTransaction(config);
        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadonlyAndAlreadyOpenedForRead_thenReadonlyException() {
        BetaLongRef ref = newLongRef(stm, 0);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).setReadonly(true);
        BetaTransaction tx = newTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasNoCommitLock(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenReadBiasedAndLock() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasCommitLock(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenUpdateBiased() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertNull(ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(0, ref);
        assertHasNoCommitLock(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenUpdateBiasedAndPrivatize() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasCommitLock(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenUpdateBiasedAndEnsure() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_UPDATE);

        assertIsActive(tx);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertNotNull(write);
        assertNotSame(read, write);
        assertSame(read, write.read);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        Tranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenAlreadyOpenedForConstructionAndLock() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = tx.openForConstruction(ref);
        constructed.value = 100;
        Tranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(constructed, write);
        assertEquals(100, constructed.value);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isPermanent);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasNoUpdates(tx);
    }

    @Test
    public void whenReadConflict() {
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read1 = tx.openForRead(ref1, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref1, LOCKMODE_NONE).value++;
        otherTx.openForWrite(ref2, LOCKMODE_NONE).value++;
        otherTx.commit();

        try {
            tx.openForWrite(ref2, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);

        assertHasNoCommitLock(ref1);
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertHasNoCommitLock(ref2);
        assertNull(ref2.___getLockOwner());
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    private void assertOpenedForWrite(BetaTransactionalObject ref, Tranlocal tranlocal) {
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertSame(ref.___unsafeLoad(), tranlocal.read);
        assertFalse(tranlocal.isCommitted);
    }

    @Test
    public void whenAlreadyOpenedForReadThenUpgraded() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref, LOCKMODE_NONE);
        Tranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertOpenedForWrite(ref, write);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenLocked_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = newTransaction();
        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaTransactionalObject ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        Tranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);
        Tranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertSame(write1, write2);
        assertIsActive(tx);
        assertAttached(tx, write1);
        assertHasUpdates(tx);
    }

    @Test
    public void multipleOpenForWrites() {
        assumeTrue(getTransactionMaxCapacity()>=3);

        BetaTransactionalObject ref1 = newLongRef(stm);
        BetaTransactionalObject ref2 = newLongRef(stm);
        BetaTransactionalObject ref3 = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        Tranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);
        Tranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);
        Tranlocal write3 = tx.openForWrite(ref3, LOCKMODE_NONE);

        assertIsActive(tx);
        assertOpenedForWrite(ref1, write1);
        assertOpenedForWrite(ref2, write2);
        assertOpenedForWrite(ref3, write3);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
        assertAttached(tx, write3);
        assertHasUpdates(tx);
    }

    @Test
    public void whenPessimisticReadEnabled() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);
        BetaTransaction tx = newTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenPessimisticWriteEnabled() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites);
        BetaTransaction tx = newTransaction(config);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertNotSame(committed, write);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, write.read);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void whenUpdatingReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertHasUpdates(tx);
    }

    @Test
    public void conflictCounterIsSetAtFirstWrite() {
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();

        stm.getGlobalConflictCounter().signalConflict(ref);
        tx.openForWrite(ref, LOCKMODE_NONE);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    public void conflictCounterIsNotSetWhenAlreadyOpenedForWrite() {
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForWrite(ref, LOCKMODE_NONE);
        long localConflictCount = tx.getLocalConflictCounter().get();
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForWrite(ref, LOCKMODE_NONE);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }

    @Test
    public void whenUnrealConflictThenConflictCounterUpdated() {
        assumeTrue(getTransactionMaxCapacity()>=3);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);
        BetaLongRef ref3 = newLongRef(stm);

        BetaTransaction tx = newTransaction();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        tx.openForWrite(ref1, LOCKMODE_NONE);

        //do second read
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        tx.openForWrite(ref2, LOCKMODE_NONE);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        //do another read
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        tx.openForWrite(ref3, LOCKMODE_NONE);
        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());

        assertIsActive(tx);
    }

    @Test
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        assumeTrue(getTransactionMaxCapacity()>=2);

        BetaLongRef ref1 = createReadBiasedLongRef(stm, 100);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(false)
                .init();

        BetaTransaction tx = newTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);

        //an unreal readconflict
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        try {
            tx.openForWrite(ref2, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSurplus(1, ref1);
        assertHasNoCommitLock(ref1);
        assertNull(ref1.___getLockOwner());
        assertSurplus(0, ref2);
        assertHasNoCommitLock(ref2);
        assertNull(ref2.___getLockOwner());
    }

    @Test
    public void commute_whenHasCommutingFunctions() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newLongIncFunction(1);
        tx.commute(ref, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(commuting, write);
        assertSame(committed, write.read);
        assertFalse(write.isCommuting);
        assertFalse(write.isCommitted);
        assertEquals(11, write.value);
        assertNull(ref.___getLockOwner());
        assertHasNoCommutingFunctions(write);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    @Test
    public void commute_whenHasCommutingFunctionAndLocked_thenReadConflict() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newLongIncFunction(1);
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }


    @Test
    public void commute_whenPessimisticThenNoConflictDetectionNeeded() {
        assumeTrue(isSupportingCommute());
        assumeTrue(hasLocalConflictCounter());
        assumeTrue(getTransactionMaxCapacity()>=2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);

        BetaTransaction tx = newTransaction(config);
        LongRefTranlocal write1 = tx.openForWrite(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        LongRefTranlocal write2 = tx.openForWrite(ref2, LOCKMODE_NONE);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
        assertAttached(tx, write1);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    @Test
    public void commute_whenCommuteAvailableThatCausesProblems_thenAbort() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void commute_whenCommuteConflicts_thenAborted() {
        assumeTrue(getTransactionMaxCapacity() >= 2);
        assumeTrue(isSupportingCommute());

        BetaLongRef ref1 = newLongRef(stm, 10);
        BetaLongRef ref2 = newLongRef(stm, 10);

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref1, LOCKMODE_NONE);
        LongFunction function = mock(LongFunction.class);
        tx.commute(ref2, function);

        ref1.atomicIncrementAndGet(1);

        try {
            tx.openForWrite(ref2, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertNull(ref1.___getLockOwner());
        assertHasNoCommitLock(ref1);
        assertSurplus(0, ref1);
        assertUpdateBiased(ref1);

        assertNull(ref2.___getLockOwner());
        assertHasNoCommitLock(ref2);
        assertSurplus(0, ref2);
        assertUpdateBiased(ref2);
    }

    @Test
    public void commute_whenCommuteAvailableThatCausesProblemsAndLock_thenAbort() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, LOCKMODE_COMMIT);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNotCommittedBefore_thenReadConflict() {
        BetaTransaction otherTx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(otherTx);

        BetaTransaction tx = newTransaction();

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
    }

    //todo:
    @Test
    public void whenReadBiasedAndPrivatize() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertReadBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasCommitLock(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    //todo:
    @Test
    public void whenUpdateBiasedAndLock() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();
        int oldReadonlyCount = ref.___getReadonlyCount();

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertIsActive(tx);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertReadonlyCount(oldReadonlyCount, ref);
        assertHasCommitLock(ref);
        assertNotNull(write);
        assertEquals(100, write.value);
        assertSame(committed, write.read);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(committed, ref.___unsafeLoad());
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

      //todo:
    @Test
    public void whenOpenForWriteAndLock() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);
        LongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    //todo:
    @Test
    public void whenAlreadyOpenedForReadAndLocked() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_COMMIT);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    //todo:
    @Test
    public void whenAlreadyOpenedForWriteAndLocked() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_COMMIT);
        LongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_NONE);

        assertSame(write2, write1);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write1);
        assertHasUpdates(tx);
    }

    //todo:
    @Test
    public void whenAlreadyOpenedForReadAndLock() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertNotNull(write);
        assertSame(read, write.read);
        assertEquals(100, write.value);
        assertSame(ref, write.owner);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write);
        assertHasUpdates(tx);
    }

    //todo:
    @Test
    public void whenAlreadyOpenedForWriteAndLock() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_NONE);
        LongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    //todo:
    @Test
    public void whenRepeatedLockThenNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        LongRefTranlocal write1 = tx.openForWrite(ref, LOCKMODE_COMMIT);
        LongRefTranlocal write2 = tx.openForWrite(ref, LOCKMODE_COMMIT);

        assertSame(write1, write2);
        assertEquals(100, write2.value);
        assertSame(ref, write2.owner);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsActive(tx);
        assertAttached(tx, write2);
        assertHasUpdates(tx);
    }

    //todo:
    @Test
    public void whenLockedByOtherAfterOpenedAndLockRequired() {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForWrite(ref, LOCKMODE_COMMIT);

        try {
            tx.openForWrite(ref, LOCKMODE_COMMIT);
            fail();
        } catch (ReadWriteConflict e) {

        }

        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsAborted(tx);
    }

    //todo:
    @Test
    public void whenLockedByOtherAndNoLockNeeded_thenReadConflict() {
        whenLockedByOther_thenReadConflict(false);
    }

    //todo:
    @Test
    public void whenLockedByOtherAndLockNeeded_thenReadConflict() {
        whenLockedByOther_thenReadConflict(true);
    }

    //todo:
    public void whenLockedByOther_thenReadConflict(boolean lockNeeded) {
        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        BetaTransaction tx = newTransaction();
        try {
            tx.openForWrite(ref, lockNeeded?LOCKMODE_COMMIT:LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
        assertIsAborted(tx);
    }

    //todo:
    @Test
    public void whenNullRef_thenNullPointerException() {
        BetaTransaction tx = newTransaction();

        try {
            tx.openForWrite((BetaLongRef) null, LOCKMODE_NONE);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
    }

    //todo:
    @Test
    public void whenHasCommutingFunctions() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newLongIncFunction(1);
        tx.commute(ref, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal write = tx.openForWrite(ref, LOCKMODE_NONE);

        assertIsActive(tx);
        assertSame(commuting, write);
        assertSame(committed, write.read);
        assertFalse(write.isCommuting);
        assertFalse(write.isCommitted);
        assertEquals(11, write.value);
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoCommutingFunctions(write);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, write);
    }

    //todo:
    @Test
    public void whenHasCommutingFunctionAndLocked_thenReadConflict() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = newTransaction();
        ref.privatize(otherTx);

        BetaTransaction tx = newTransaction();
        LongFunction function = Functions.newLongIncFunction(1);
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    //todo:
    @Test
    public void whenCommuteAvailableThatCausesProblems_thenAbort() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    //todo:
    @Test
    public void whenCommuteAvailableThatCausesProblemsAndLock_thenAbort() {
        assumeTrue(isSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, LOCKMODE_COMMIT);
            fail();
        } catch (RuntimeException e) {
            assertSame(exception, e);
        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.prepare();

        BetaLongRef ref = newLongRef(stm);
        try {
            tx.openForWrite(ref, LOCKMODE_NONE);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.abort();

        BetaLongRef ref = newLongRef(stm);

        try {
            tx.openForWrite(ref, LOCKMODE_COMMIT);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        BetaLongRef ref = newLongRef(stm);

        try {
            tx.openForWrite(ref, LOCKMODE_COMMIT);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

}

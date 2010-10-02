package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public abstract class BetaTransaction_openForConstructionTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    protected abstract boolean hasLocalConflictCounter();

    protected abstract int getMaxTransactionCapacity();

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenSuccess() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal write = tx.openForConstruction(ref);

        assertIsActive(tx);
        assertAttached(tx, write);
        assertNotNull(write);
        assertEquals(0, write.value);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertEquals(DIRTY_TRUE, write.isDirty);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal construction1 = tx.openForConstruction(ref);
        LongRefTranlocal construction2 = tx.openForConstruction(ref);

        assertIsActive(tx);
        assertAttached(tx, construction1);
        assertSame(construction1, construction2);
        assertNotNull(construction1);
        assertEquals(0, construction1.value);
        assertSame(ref, construction1.owner);
        assertNull(construction1.read);
        assertFalse(construction1.isCommitted);
        assertFalse(construction1.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertEquals(DIRTY_TRUE, construction1.isDirty);
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        BetaTransaction tx = newTransaction();

        try {
            tx.openForConstruction((BetaLongRef) null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted_thenIllegalArgumentException() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForReading_thenIllegalArgumentException() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        tx.openForRead(ref, LOCKMODE_NONE);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite_thenIllegalArgumentException() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = newTransaction();
        tx.openForWrite(ref, LOCKMODE_NONE);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (IllegalArgumentException expected) {
        }

        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenReadonly_thenReadonlyException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);

        BetaTransaction tx = newTransaction(config);

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertSame(committed, ref.___unsafeLoad());
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertReadonlyCount(0, ref);
    }

    @Test
    public void whenPessimisticThenNoConflictDetectionNeeded() {
        assumeTrue(getMaxTransactionCapacity()>2);
        assumeTrue(hasLocalConflictCounter());

        BetaLongRef ref1 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads)
                .init();

        BetaTransaction tx = newTransaction(config);
        tx.openForRead(ref1, LOCKMODE_NONE);

        long oldLocalConflictCount = tx.getLocalConflictCounter().get();

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        BetaLongRef ref2 = new BetaLongRef(tx);
        tx.openForConstruction(ref2);

        assertEquals(oldLocalConflictCount, tx.getLocalConflictCounter().get());
    }

    @Test
    public void conflictCounterIsNotReset() {
        assumeTrue(hasLocalConflictCounter());

        BetaTransaction tx = newTransaction();
        long oldConflictCount = tx.getLocalConflictCounter().get();
        BetaLongRef ref = new BetaLongRef(tx);

        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));
        tx.openForConstruction(ref);

        assertEquals(oldConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
    }
   
    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.prepare();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.abort();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        tx.commit();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}

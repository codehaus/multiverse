package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatMonoBetaTransaction_openForConstructionTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenSuccess() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);

        LongRefTranlocal write = tx.openForConstruction(ref);

        assertIsActive(tx);
        assertNotNull(write);
        assertEquals(0, write.value);
        assertSame(ref, write.owner);
        assertNull(write.read);
        assertFalse(write.isCommitted);
        assertFalse(write.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertAttached(tx, write);
        assertEquals(DIRTY_TRUE, write.isDirty);
    }

    @Test
    public void whenOverflowing() {
        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm);
        config.init();
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        BetaLongRef ref1 = new BetaLongRef(tx);
        BetaLongRef ref2 = new BetaLongRef(tx);

        tx.openForConstruction(ref1);
        try {
            tx.openForConstruction(ref2);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertIsAborted(tx);
        assertEquals(2, config.getSpeculativeConfiguration().minimalLength);
    }

    @Test
    public void whenNullRef_thenNullPointerException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        try {
            tx.openForConstruction((BetaLongRef) null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyCommitted() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

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
    public void whenAlreadyOpenedForConstruction() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);

        LongRefTranlocal write1 = tx.openForConstruction(ref);
        LongRefTranlocal write2 = tx.openForConstruction(ref);

        assertSame(write1, write2);
        assertIsActive(tx);
        assertNotNull(write2);
        assertEquals(0, write2.value);
        assertSame(ref, write2.owner);
        assertNull(write2.read);
        assertFalse(write2.isCommitted);
        assertFalse(write2.isPermanent);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertAttached(tx, write1);
        assertEquals(DIRTY_TRUE, write1.isDirty);
    }

    @Test
    public void whenAlreadyOpenedForReading_thenIllegalArgumentException() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);

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
    public void conflictCounterIsNotReset() {
        BetaLongRef ref = newLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        stm.getGlobalConflictCounter().signalConflict(ref);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);

        assertEquals(stm.getGlobalConflictCounter().count(), tx.getLocalConflictCounter().get());
        assertIsActive(tx);
        assertAttached(tx, read);
    }

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    @Ignore
    public void whenUndefined() {
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
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

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit();

        try {
            tx.openForConstruction(ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}

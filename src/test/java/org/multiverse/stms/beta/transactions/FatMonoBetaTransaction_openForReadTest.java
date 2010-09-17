package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.functions.IncLongFunction;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

/**
 * @author Peter Veentjer
 */
public class FatMonoBetaTransaction_openForReadTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void whenUndefined() {

    }

    @Test
    public void whenUntracked() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false);

        assertIsActive(tx);
        assertSame(committed, read);
        assertNull(tx.get(ref));
        assertTrue((Boolean) getField(tx, "hasReads"));
        assertTrue((Boolean) getField(tx, "hasUntrackedReads"));
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertNotAttached(tx, ref);
    }

    @Test
    public void whenNull() {
        BetaTransaction tx = new FatMonoBetaTransaction(stm);
        Tranlocal result = tx.openForRead((BetaLongRef) null, true);

        assertNull(result);
        assertIsActive(tx);
    }

    @Test
    public void whenOverflowing() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm).init();
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForRead(ref1, false);
        try {
            tx.openForRead(ref2, false);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertIsAborted(tx);
        assertEquals(2, config.getSpeculativeConfiguration().minimalLength);
    }

    @Test
    public void whenUpdateBiased() {
        BetaLongRef ref = newLongRef(stm, 10);
        Tranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);

        assertIsActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref.___getOrec());
        assertHasNoCommitLock(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertSame(ref, read.owner);
        assertTrue(committed.isCommitted);
        assertFalse(committed.isPermanent);
        assertEquals(10, read.value);
        assertAttached(tx, read);
    }

    @Test
    public void whenReadBiased() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, false);

        assertIsActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref);
        assertHasNoCommitLock(ref);
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertTrue(read.isCommitted);
        assertTrue(read.isPermanent);
        assertEquals(10, read.value);
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForRead() {
        BetaLongRef ref = newLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref, false);
        LongRefTranlocal read2 = tx.openForRead(ref, false);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertHasNoCommitLock(ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertAttached(tx, read2);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm, 10);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal write = tx.openForWrite(ref, false);
        LongRefTranlocal read = tx.openForRead(ref, false);

        assertIsActive(tx);
        assertSame(write, read);
        assertHasNoCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertNull(ref.___getLockOwner());
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyOpenedForConstruction() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal construction = tx.openForConstruction(ref);
        LongRefTranlocal read = tx.openForRead(ref, false);

        assertIsActive(tx);
        assertSame(construction, read);
        assertHasCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertNull(ref.___unsafeLoad());
        assertAttached(tx, read);
    }

    @Test
    public void whenConstructedAndLock() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal construction = tx.openForConstruction(ref);

        LongRefTranlocal read = tx.openForRead(ref, true);

        assertIsActive(tx);
        assertSame(construction, read);
        assertNull(ref.___unsafeLoad());
        assertHasCommitLock(ref);
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertAttached(tx, read);
    }

    @Test
    public void whenLockedByOther_thenLockedConflict() {
        BetaLongRef ref = newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, true);

        FatMonoBetaTransaction tx2 = new FatMonoBetaTransaction(stm);
        try {
            tx2.openForRead(ref, false);
            fail();
        } catch (ReadConflict expected) {
        }

        assertIsAborted(tx2);
        assertSame(committed, ref.___unsafeLoad());
        assertHasCommitLock(ref.___getOrec());
        assertReadonlyCount(0, ref.___getOrec());
        assertUpdateBiased(ref.___getOrec());
        assertSurplus(1, ref.___getOrec());
        assertSame(otherTx, ref.___getLockOwner());
    }

    @Test
    public void whenLock() {
        BetaLongRef ref = newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, true);

        assertIsActive(tx);
        assertSame(committed, read);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read);
    }

    @Test
    public void whenAlreadyLockedBySelf_thenNoProblem() {
        BetaLongRef ref = newLongRef(stm);
        Tranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read1 = tx.openForRead(ref, true);
        LongRefTranlocal read2 = tx.openForRead(ref, true);

        assertIsActive(tx);
        assertSame(read1, read2);
        assertSame(committed, read2);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertUpdateBiased(ref);
        assertSurplus(1, ref);
        assertAttached(tx, read2);
    }

    @Test
    public void whenReadBiasedAndNoReadTrackingAndLock_thenAttached() {
        BetaLongRef ref = createReadBiasedLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setBlockingAllowed(false)
                .setReadTrackingEnabled(false);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, true);

        assertIsActive(tx);
        assertSame(committed, read);
        assertSurplus(1, ref);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertReadBiased(ref);
        assertReadonlyCount(0, ref);
        assertTrue(read.isCommitted);
        assertTrue(read.isPermanent);
        assertEquals(10, read.value);
        assertAttached(tx, read);
    }

    @Test
    public void whenContainsUntrackedRead_thenCantRecoverFromUnrealReadConflict() {
        BetaLongRef ref1 = createReadBiasedLongRef(stm, 100);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadTrackingEnabled(false);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);
        tx.openForRead(ref1, false);

        //an unreal readconflict
        stm.getGlobalConflictCounter().signalConflict(newLongRef(stm));

        try {
            tx.openForRead(ref2, false);
            fail();
        } catch (ReadConflict expected) {
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
    public void whenPessimisticReadEnabled() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeReads);
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false);

        assertIsActive(tx);
        assertSame(committed, read);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
        assertAttached(tx, read);
    }

    @Test
    public void whenPessimisticWriteEnabled() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setPessimisticLockLevel(PessimisticLockLevel.PrivatizeWrites);
        BetaTransaction tx = new FatMonoBetaTransaction(config);
        LongRefTranlocal read = tx.openForRead(ref, false);

        assertIsActive(tx);
        assertSame(committed, read);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertTrue(read.isCommitted);
        assertFalse(read.isPermanent);
        assertAttached(tx, read);
    }

    @Test
    public void commute_whenHasCommutingFunctions() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        LongRefTranlocal read = tx.openForWrite(ref, false);

        assertIsActive(tx);
        assertSame(commuting, read);
        assertSame(committed, read.read);
        assertFalse(read.isCommuting);
        assertFalse(read.isCommitted);
        assertEquals(11, read.value);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertAttached(tx, read);
    }

    @Test
    public void commute_whenHasCommutingFunctionsAndLocked_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction otherTx = new FatMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongFunction function = new IncLongFunction();
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, false);
            fail();
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void commute_whenCommuteAvailableThatCausesProblems_thenAbort() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, false);
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
    public void commute_whenCommuteAvailableThatCausesProblemsAndLock_thenAbort() {
        BetaLongRef ref = newLongRef(stm, 10);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongFunction function = mock(LongFunction.class);
        RuntimeException exception = new RuntimeException();
        when(function.call(10)).thenThrow(exception);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, function);

        try {
            tx.openForRead(ref, true);
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
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare();

        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            tx.openForRead(ref, true);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertSame(committed, ref.___unsafeLoad());
        assertUpdateBiased(ref);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort();

        BetaLongRef ref = newLongRef(stm);

        try {
            tx.openForRead(ref, true);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit();

        BetaLongRef ref = newLongRef(stm);

        try {
            tx.openForRead(ref, true);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}

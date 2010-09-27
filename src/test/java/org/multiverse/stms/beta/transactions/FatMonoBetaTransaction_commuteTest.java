package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class FatMonoBetaTransaction_commuteTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        try {
            tx.commute(ref, null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNotOpenedBefore() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        LongFunction function = mock(LongFunction.class);

        tx.commute(ref, function);

        assertIsActive(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
    }

    @Test
    public void whenAlreadyOpenedForCommute() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);

        tx.commute(ref, function1);
        tx.commute(ref, function2);

        assertIsActive(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function2, function1);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        verifyZeroInteractions(function1);
        verifyZeroInteractions(function2);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenOpenedForWritingAndApplyFunction() {
        BetaLongRef ref = newLongRef(stm, 100);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        tx.commute(ref, Functions.newIncLongFunction(1));

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertIsActive(tx);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertSame(read, tranlocal.read);
        assertEquals(101, tranlocal.value);
        assertHasNoCommutingFunctions(tranlocal);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        LongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commute(ref, Functions.newIncLongFunction(1));

        assertIsActive(tx);
        assertSame(ref, tranlocal.owner);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertSame(committed, tranlocal.read);
        assertEquals(101, tranlocal.value);
        assertHasNoCommutingFunctions(tranlocal);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(1, ref);
    }

    @Test
    @Ignore
    public void whenPessimisticTransactionAndNotOpenedBefore_thenIgnorePessimisticSetting() {

    }

    @Test
    public void whenFreshObject() {
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);
        tx.commute(ref, Functions.newIncLongFunction(1));

        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);

        assertIsActive(tx);
        assertSame(ref, tranlocal.owner);
        assertFalse(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertEquals(1, tranlocal.value);
        assertHasNoCommutingFunctions(tranlocal);
        assertHasCommitLock(ref);
        assertSame(tx, ref.___getLockOwner());
        assertSurplus(1, ref);
    }

    @Test
    public void whenOverflow() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        tx.commute(ref1, function1);

        try {
            tx.commute(ref2, function2);
            fail();
        } catch (SpeculativeConfigurationError expected) {

        }

        assertIsAborted(tx);
        assertEquals(2, tx.getConfiguration().getSpeculativeConfiguration().minimalLength);

        verifyZeroInteractions(function1);
        assertSurplus(0, ref1);
        assertHasNoCommitLock(ref1);

        verifyZeroInteractions(function2);
        assertSurplus(0, ref2);
        assertHasNoCommitLock(ref2);
    }

    @Test
    public void whenCommuteThenConflictCounterNotSet() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        long localConflictCount = tx.getLocalConflictCounter().get();

        LongFunction function = mock(LongFunction.class);

        stm.getGlobalConflictCounter().signalConflict(new BetaLongRef(stm));

        tx.commute(ref, function);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
    }

    @Test
    public void whenMultipleCommutesOnSameReference() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        LongFunction function3 = mock(LongFunction.class);

        tx.commute(ref, function1);
        tx.commute(ref, function2);
        tx.commute(ref, function3);

        assertIsActive(tx);
        LongRefTranlocal tranlocal = (LongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isCommitted);
        assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function3, function2, function1);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
    }

    @Test
    public void whenPrivatizedByOther_thenNoProblem() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        LongFunction function = mock(LongFunction.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        assertIsActive(tx);
        assertTrue(commuting.isCommuting);
        assertFalse(commuting.isCommitted);
        assertNull(commuting.read);
        assertHasCommutingFunctions(commuting, function);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenEnsuredByOther_thenNoProblem() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        LongFunction function = mock(LongFunction.class);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commute(ref, function);

        LongRefTranlocal commuting = (LongRefTranlocal) tx.get(ref);

        assertIsActive(tx);
        assertTrue(commuting.isCommuting);
        assertFalse(commuting.isCommitted);
        assertNull(commuting.read);
        assertHasCommutingFunctions(commuting, function);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadonlyTransaction_thenReadonlyException() {
        BetaLongRef ref = newLongRef(stm);

        LongFunction function = mock(LongFunction.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);
        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(config);

        try {
            tx.commute(ref, function);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenPrepared_thenPreparedException() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.prepare();

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, function);
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
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.commit();

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);

        FatMonoBetaTransaction tx = new FatMonoBetaTransaction(stm);
        tx.abort();

        LongFunction function = mock(LongFunction.class);

        try {
            tx.commute(ref, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }
}

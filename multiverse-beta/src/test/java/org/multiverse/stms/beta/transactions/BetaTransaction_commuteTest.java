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
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRefTranlocal;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

public abstract class BetaTransaction_commuteTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);

    public abstract boolean isTransactionSupportingCommute();

    public abstract boolean hasLocalConflictCounter();

    public abstract int getTransactionMaxCapacity();

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullFunction_thenNullPointerException() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();

        try {
            tx.commute(ref, null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenNotOpenedBefore() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();

        LongFunction function = mock(LongFunction.class);

        tx.commute(ref, function);

        assertIsActive(tx);
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting());
        assertFalse(tranlocal.isReadonly());
        //todo:
        //assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
    }

    @Test
    public void whenCommuteThenConflictCounterNotSet() {
        assumeTrue(hasLocalConflictCounter());
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
        long localConflictCount = tx.getLocalConflictCounter().get();

        LongFunction function = mock(LongFunction.class);

        stm.getGlobalConflictCounter().signalConflict(new BetaLongRef(stm));

        tx.commute(ref, function);

        assertEquals(localConflictCount, tx.getLocalConflictCounter().get());
        assertIsActive(tx);
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting());
        assertFalse(tranlocal.isReadonly());
        //todo:
        //assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
    }

    @Test
    public void whenMultipleCommutesOnSameReference() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        LongFunction function3 = mock(LongFunction.class);

        tx.commute(ref, function1);
        tx.commute(ref, function2);
        tx.commute(ref, function3);

        assertIsActive(tx);
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting());
        assertFalse(tranlocal.isReadonly());
        //todo:
        //assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function3, function2, function1);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
    }

    @Test
    public void whenMultipleCommutesInTransaction() {
        assumeTrue(isTransactionSupportingCommute());
        assumeTrue(getTransactionMaxCapacity() >= 2);

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx = newTransaction();

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);

        tx.commute(ref1, function1);
        tx.commute(ref2, function2);

        assertIsActive(tx);
        BetaLongRefTranlocal tranlocal1 = (BetaLongRefTranlocal) tx.get(ref1);
        assertHasCommutingFunctions(tranlocal1, function1);
        BetaLongRefTranlocal tranlocal2 = (BetaLongRefTranlocal) tx.get(ref2);
        assertHasCommutingFunctions(tranlocal2, function2);
    }


    @Test
    public void whenOverflow() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm, 1);
        config.init();
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(config);

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
        assertRefHasNoLocks(ref1);

        verifyZeroInteractions(function2);
        assertSurplus(0, ref2);
        assertRefHasNoLocks(ref2);
    }


    @Test
    public void whenAlreadyOpenedForCommute() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();

        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);

        tx.commute(ref, function1);
        tx.commute(ref, function2);

        assertIsActive(tx);
        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);

        assertNotNull(tranlocal);
        assertSame(ref, tranlocal.owner);
        assertTrue(tranlocal.isCommuting());
        assertFalse(tranlocal.isReadonly());
        //todo:
        // assertNull(tranlocal.read);
        assertHasCommutingFunctions(tranlocal, function2, function1);
        assertRefHasNoLocks(ref);
        assertSurplus(0, ref);
        verifyZeroInteractions(function1);
        verifyZeroInteractions(function2);
    }

    @Test
    public void whenAlreadyOpenedForRead_thenOpenedForWritingAndApplyFunction() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 100);

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal read = tx.openForRead(ref, LOCKMODE_NONE);
        tx.commute(ref, Functions.newIncLongFunction(1));

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertIsActive(tx);
        assertFalse(tranlocal.isCommuting());
        assertFalse(tranlocal.isReadonly());
        //todo:
        //assertSame(read, tranlocal.read);
        assertEquals(101, tranlocal.value);
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = newTransaction();
        BetaLongRefTranlocal tranlocal = tx.openForWrite(ref, LOCKMODE_NONE);
        tx.commute(ref, Functions.newIncLongFunction(1));

        assertIsActive(tx);
        assertSame(ref, tranlocal.owner);
        assertFalse(tranlocal.isCommuting());
        assertFalse(tranlocal.isReadonly());
        assertEquals(101, tranlocal.value);
        assertRefHasNoLocks(ref);
        assertSurplus(1, ref);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    @Ignore
    public void whenPessimisticTransactionAndNotOpenedBefore_thenIgnorePessimisticSetting() {

    }

    @Test
    public void whenFreshObject() {
        assumeTrue(isTransactionSupportingCommute());

        BetaTransaction tx = newTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        tx.openForConstruction(ref);
        tx.commute(ref, Functions.newIncLongFunction(1));

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);

        assertIsActive(tx);
        assertSame(ref, tranlocal.owner);
        assertFalse(tranlocal.isCommuting());
        assertFalse(tranlocal.isReadonly());
        //todo:
        //assertNull(tranlocal.read);
        assertEquals(1, tranlocal.value);
        assertHasNoCommutingFunctions(tranlocal);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
    }

    @Test
    public void whenPrivatizedByOther_thenNoProblem() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        LongFunction function = mock(LongFunction.class);
        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        BetaLongRefTranlocal commuting = (BetaLongRefTranlocal) tx.get(ref);

        assertIsActive(tx);
        assertTrue(commuting.isCommuting());
        assertFalse(commuting.isReadonly());
        //todo:
        // assertNull(commuting.read);
        assertHasCommutingFunctions(commuting, function);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenEnsuredByOther_thenNoProblem() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(otherTx);

        LongFunction function = mock(LongFunction.class);
        BetaTransaction tx = newTransaction();
        tx.commute(ref, function);

        BetaLongRefTranlocal commuting = (BetaLongRefTranlocal) tx.get(ref);

        assertIsActive(tx);
        assertTrue(commuting.isCommuting());
        assertFalse(commuting.isReadonly());
        //todo:
        //assertNull(commuting.read);
        assertHasCommutingFunctions(commuting, function);
        assertRefHasUpdateLock(ref,otherTx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenReadonlyTransaction_thenReadonlyException() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        LongFunction function = mock(LongFunction.class);

        BetaTransactionConfiguration config = new BetaTransactionConfiguration(stm)
                .setReadonly(true);
        BetaTransaction tx = newTransaction(config);

        try {
            tx.commute(ref, function);
            fail();
        } catch (ReadonlyException expected) {
        }

        assertIsAborted(tx);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenAlreadyPrepared_thenPreparedTransactionException() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
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
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
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
        assumeTrue(isTransactionSupportingCommute());

        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = newTransaction();
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

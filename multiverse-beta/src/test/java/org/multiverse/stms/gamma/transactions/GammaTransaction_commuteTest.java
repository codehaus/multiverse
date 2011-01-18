package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.*;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.GammaTestUtils;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.LOCKMODE_NONE;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

public abstract class GammaTransaction_commuteTest<T extends GammaTransaction> {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    protected abstract int getMaxCapacity();

    @Test
    public void whenTransactionAbortOnly_thenWriteStillPossible() {
        GammaLongRef ref = new GammaLongRef(stm, 0);

        GammaTransaction tx = stm.startDefaultTransaction();
        tx.setAbortOnly();
        ref.commute(tx, Functions.newIncLongFunction());

        GammaRefTranlocal tranlocal = tx.locate(ref);
        assertNotNull(tranlocal);
        assertEquals(TRANLOCAL_COMMUTING, tranlocal.getMode());
        assertTrue(tx.isAbortOnly());
        assertIsActive(tx);
    }


    @Test
    public void whenMultipleCommutesOnSingleRef() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        ref.commute(tx, Functions.newIncLongFunction());
        ref.commute(tx, Functions.newIncLongFunction());
        ref.commute(tx, Functions.newIncLongFunction());
        tx.commit();

        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 3);

    }

    @Test
    public void whenAlreadyOpenedForRead() {
        whenAlreadyOpenedForRead(LockMode.None);
        whenAlreadyOpenedForRead(LockMode.Read);
        whenAlreadyOpenedForRead(LockMode.Write);
        whenAlreadyOpenedForRead(LockMode.Exclusive);
    }

    public void whenAlreadyOpenedForRead(LockMode lockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.version;

        GammaTransaction tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForRead(tx, lockMode.asInt());
        LongFunction incFunction = Functions.newIncLongFunction();
        ref.commute(tx, incFunction);

        assertEquals(initialValue + 1, tranlocal.long_value);
        assertTrue(tx.hasWrites);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertLockMode(ref, lockMode);
        assertTrue(tranlocal.isWrite());
        assertNull(tranlocal.headCallable);
    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForReadAndFunctionCausesProblem() {

    }

    @Test
    public void whenAlreadyOpenedForWrite() {
        whenAlreadyOpenedForWrite(LockMode.None);
        whenAlreadyOpenedForWrite(LockMode.Read);
        whenAlreadyOpenedForWrite(LockMode.Write);
        whenAlreadyOpenedForWrite(LockMode.Exclusive);
    }

    public void whenAlreadyOpenedForWrite(LockMode lockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.version;

        GammaTransaction tx = newTransaction();
        GammaRefTranlocal tranlocal = ref.openForWrite(tx, lockMode.asInt());
        LongFunction incFunction = Functions.newIncLongFunction();
        ref.commute(tx, incFunction);

        assertEquals(initialValue + 1, tranlocal.long_value);
        assertTrue(tx.hasWrites);
        assertIsActive(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertLockMode(ref, lockMode);
        assertTrue(tranlocal.isWrite());
        assertNull(tranlocal.headCallable);
    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForWriteAndFunctionsCausesProblem() {

    }

    @Test
    public void whenNotOpenedBefore() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        LongFunction function = mock(LongFunction.class);
        ref.commute(tx, function);
        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);

        assertNotNull(tranlocal);
        assertTrue(tranlocal.isCommuting());
        assertSame(ref, tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertIsActive(tx);
        GammaTestUtils.assertHasCommutingFunctions(tranlocal, function);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAlreadyOpenedForCommute() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        ref.commute(tx, function1);
        ref.commute(tx, function2);
        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);

        assertNotNull(tranlocal);
        assertTrue(tranlocal.isCommuting());
        assertSame(ref, tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertIsActive(tx);
        GammaTestUtils.assertHasCommutingFunctions(tranlocal, function2, function1);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void lockedByOther() {
        lockedByOther(LockMode.None);
        lockedByOther(LockMode.Read);
        lockedByOther(LockMode.Write);
        lockedByOther(LockMode.Exclusive);
    }

    public void lockedByOther(LockMode otherLockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquire(otherTx, otherLockMode);

        GammaTransaction tx = stm.startDefaultTransaction();
        LongFunction function1 = mock(LongFunction.class);
        LongFunction function2 = mock(LongFunction.class);
        ref.commute(tx, function1);
        ref.commute(tx, function2);
        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);

        assertNotNull(tranlocal);
        assertTrue(tranlocal.isCommuting());
        assertSame(ref, tranlocal.owner);
        assertEquals(LOCKMODE_NONE, tranlocal.getLockMode());
        assertIsActive(tx);
        GammaTestUtils.assertHasCommutingFunctions(tranlocal, function2, function1);
        assertRefHasLockMode(ref, otherTx, otherLockMode.asInt());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForConstruction() {

    }

    @Test
    @Ignore
    public void whenAlreadyOpenedForConstructionAndFunctionCausesProblem() {

    }

    @Test
    public void whenOverflowing() {
        int maxCapacity = getMaxCapacity();
        assumeTrue(maxCapacity < Integer.MAX_VALUE);

        GammaTransaction tx = newTransaction();
        for (int k = 0; k < maxCapacity; k++) {
            GammaLongRef ref = new GammaLongRef(stm, 0);
            ref.openForRead(tx, LOCKMODE_NONE);
        }

        GammaLongRef ref = new GammaLongRef(stm, 0);
        try {
            ref.commute(tx, Functions.newIncLongFunction());
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertEquals(TransactionStatus.Aborted, tx.getStatus());
        assertEquals(maxCapacity + 1, tx.getConfiguration().getSpeculativeConfiguration().minimalLength);
    }

    @Test
    public void whenNullTransaction() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = mock(LongFunction.class);

        try {
            ref.commute((ArrayGammaTransaction) null, function);
            fail();
        } catch (NullPointerException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertLockMode(ref, LOCKMODE_NONE);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenNullFunction() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setReadonly(true);

        GammaTransaction tx = newTransaction(config);

        try {
            ref.commute(tx, null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertLockMode(ref, LOCKMODE_NONE);
    }

    @Test
    public void whenReadonlyTransaction_thenReadonlyException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm)
                .setReadonly(true);

        GammaTransaction tx = newTransaction(config);
        LongFunction function = mock(LongFunction.class);

        try {
            ref.commute(tx, function);
            fail();
        } catch (ReadonlyException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertLockMode(ref, LOCKMODE_NONE);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenStmMismatch() {
        GammaStm otherStm = new GammaStm();
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(otherStm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        LongFunction function = mock(LongFunction.class);
        try {
            ref.commute(tx, function);
            fail();
        } catch (StmMismatchException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        LongFunction function = mock(LongFunction.class);

        tx.prepare();
        try {
            ref.commute(tx, function);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertLockMode(ref, LOCKMODE_NONE);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        LongFunction function = mock(LongFunction.class);

        tx.abort();
        try {
            ref.commute(tx, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertLockMode(ref, LOCKMODE_NONE);
        verifyZeroInteractions(function);
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = newTransaction();
        LongFunction function = mock(LongFunction.class);

        tx.commit();
        try {
            ref.commute(tx, function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertLockMode(ref, LOCKMODE_NONE);
        verifyZeroInteractions(function);
    }
}

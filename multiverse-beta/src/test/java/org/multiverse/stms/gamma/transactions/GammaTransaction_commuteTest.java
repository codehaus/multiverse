package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.LockMode;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaTranlocal;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;
import static org.multiverse.stms.gamma.GammaTestUtils.assertLockMode;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;

public abstract class GammaTransaction_commuteTest<T extends GammaTransaction> {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    protected abstract T newTransaction(GammaTransactionConfiguration config);

    @Test
    public void whenAlreadyOpenedForRead() {
        whenAlreadyOpenedForRead(LockMode.None);
        whenAlreadyOpenedForRead(LockMode.Read);
        whenAlreadyOpenedForRead(LockMode.Write);
        whenAlreadyOpenedForRead(LockMode.Commit);
    }

    public void whenAlreadyOpenedForRead(LockMode lockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.version;

        GammaTransaction tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForRead(tx, lockMode.asInt());
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
        whenAlreadyOpenedForWrite(LockMode.Commit);
    }

    public void whenAlreadyOpenedForWrite(LockMode lockMode) {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.version;

        GammaTransaction tx = newTransaction();
        GammaTranlocal tranlocal = ref.openForWrite(tx, lockMode.asInt());
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
    @Ignore
    public void whenAlreadyOpenedForCommute() {

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

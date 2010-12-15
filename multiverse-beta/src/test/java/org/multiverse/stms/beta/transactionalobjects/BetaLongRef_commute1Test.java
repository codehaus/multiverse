package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

public class BetaLongRef_commute1Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenActiveTransactionAvailable() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        LongFunction function = Functions.newIncLongFunction(1);
        ref.commute(function);

        BetaLongRefTranlocal commuting = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(commuting);
        assertTrue(commuting.isCommuting());
        assertFalse(commuting.isReadonly());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertEquals(0, commuting.value);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        tx.commit();

        assertEquals(1, ref.atomicGet());
        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailableAndNoChange() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        LongFunction function = Functions.newIdentityLongFunction();
        ref.commute(function);

        BetaLongRefTranlocal commuting = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(commuting);
        assertTrue(commuting.isCommuting());
        assertFalse(commuting.isReadonly());
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertEquals(0, commuting.value);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        tx.commit();

        assertEquals(0, ref.atomicGet());
        assertVersionAndValue(ref, version, 0);
        assertIsCommitted(tx);
        assertSurplus(0, ref);
        assertRefHasNoLocks(ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailableAndNullFunction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.commute(null);
            fail();
        } catch (NullPointerException expected) {
        }


        assertIsAborted(tx);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = Functions.newIncLongFunction(1);
        try {
            ref.commute(function);
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        LongFunction function = Functions.newIncLongFunction(1);
        try {
            ref.commute(function);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        LongFunction function = Functions.newIncLongFunction(1);
        try {
            ref.commute(function);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 2);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.prepare();

        LongFunction function = Functions.newIncLongFunction(1);
        try {
            ref.commute(function);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 2);
        assertEquals(2, ref.atomicGet());
    }

    @Test
    public void whenAlreadyEnsuredBySelf_thenNoCommute() {
        BetaLongRef ref = newLongRef(stm, 2);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure();
        LongFunction function = Functions.newIncLongFunction(1);
        ref.commute(function);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertFalse(tranlocal.isCommuting());
        assertEquals(3, tranlocal.value);
        assertIsActive(tx);
        assertRefHasUpdateLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);

        tx.commit();

        assertSurplus(0, ref);
        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(3, ref.atomicGet());
    }

    @Test
    public void whenAlreadyPrivatizedBySelf_thenNoCommute() {
        BetaLongRef ref = newLongRef(stm, 2);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();
        LongFunction function = Functions.newIncLongFunction(1);
        ref.commute(function);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertFalse(tranlocal.isCommuting());
        assertEquals(3, tranlocal.value);
        assertIsActive(tx);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);

        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(3, ref.atomicGet());
        assertSurplus(0, ref);
    }

    @Test
    public void whenEnsuredByOther_thenCommuteSucceedsButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 2);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        LongFunction function = Functions.newIncLongFunction(1);
        ref.commute(function);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertTrue(tranlocal.isCommuting());
        assertHasCommutingFunctions(tranlocal, function);
        assertIsActive(tx);
        assertRefHasUpdateLock(ref,otherTx);
        assertSurplus(1, ref);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertRefHasUpdateLock(ref, otherTx);
        assertVersionAndValue(ref, version, 2);
        assertSurplus(1, ref);
    }

    @Test
    public void whenPrivatizedByOther_thenCommuteSucceedsButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 2);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        LongFunction function = Functions.newIncLongFunction(1);
        ref.commute(function);

        BetaLongRefTranlocal tranlocal = (BetaLongRefTranlocal) tx.get(ref);
        assertNotNull(tranlocal);
        assertTrue(tranlocal.isCommuting());
        assertHasCommutingFunctions(tranlocal, function);
        assertIsActive(tx);
        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(1, ref);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertRefHasCommitLock(ref, otherTx);
        assertVersionAndValue(ref, version, 2);
        assertSurplus(1, ref);
    }
}

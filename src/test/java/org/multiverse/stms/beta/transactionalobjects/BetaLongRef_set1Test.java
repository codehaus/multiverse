package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConfiguration;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_set1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        BetaStmConfiguration configuration = new BetaStmConfiguration();
        configuration.maxRetries = 10;
        stm = new BetaStm(configuration);
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.set(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertIsAborted(tx);
        assertEquals(10, ref.atomicGet());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenActiveTransactionAvailable_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.set(20);

        assertIsActive(tx);
        assertEquals(20, value);
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);

        tx.commit();

        assertIsCommitted(tx);
        assertEquals(20, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenLocked_thenReadWriteConflict() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction otherTx = stm.startDefaultTransaction();
        otherTx.openForRead(ref, LOCKMODE_COMMIT);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.set(20);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);

        otherTx.abort();
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenNoChange() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        long result = ref.set(initialValue);

        tx.commit();

        assertIsCommitted(tx);
        assertEquals(initialValue, result);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNoTransactionFound_thenNoTransactionFoundException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.set(20);
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedBySelf_thenSuccess() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();
        long value = ref.set(200);

        assertEquals(200, value);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenEnsuredBySelf_thenSuccess() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure();
        long value = ref.set(200);

        assertEquals(200, value);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenPrivatizedByOtherAndFirstTimeRead_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.set(200);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenEnsuredByOther_thenSetPossibleButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        long value = ref.set(200);
        assertEquals(200, value);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict e) {

        }

        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenExecutedAtomically() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, 10);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        try {
            ref.set(initialValue + 1);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        try {
            ref.set(20);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
    }
}

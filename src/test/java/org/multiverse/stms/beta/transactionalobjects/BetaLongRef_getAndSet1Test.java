package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_getAndSet1Test {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenActiveTransactionAvailable() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndSet(initialValue + 2);
        tx.commit();

        assertEquals(initialValue, value);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion + 1, initialValue + 2);
    }

    @Test
    public void whenNoChange() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.getAndSet(initialValue);
        tx.commit();

        assertEquals(initialValue, value);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure();
        long result = ref.getAndSet(20);

        assertEquals(10, result);
        assertIsActive(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenPrivatizedBySelf() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();
        long result = ref.getAndSet(20);

        assertEquals(10, result);
        assertIsActive(tx);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenEnsuredByOther_thenGetAndSetSucceedsButCommitFails() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();

        ref.ensure(otherTx);

        long result = ref.getAndSet(20);
        assertEquals(10, result);
        assertIsActive(tx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(2, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertIsActive(otherTx);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 10);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.getAndSet(20);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {
    }

    @Test
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, 10);
        long initialVersion = ref.getVersion();

        long newValue = 20;
        try {
            ref.getAndSet(newValue);
            fail();
        } catch (TransactionRequiredException expected) {
        }

        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.getAndSet(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        try {
            ref.getAndSet(initialValue + 1);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        setThreadLocalTransaction(tx);

        try {
            ref.getAndSet(initialValue + 1);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}

package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertSurplus;
import static org.multiverse.stms.beta.orec.OrecTestUtils.assertUpdateBiased;

public class VeryAbstractTransactionalObject_privatizeTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        try {
            ref.acquireCommitLock();
            fail();
        } catch (TransactionRequiredException expected) {
        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenNoTransactionFoundException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        Transaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        try {
            ref.acquireCommitLock();
            fail();
        } catch (DeadTransactionException expected) {
        }


        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        Transaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        try {
            ref.acquireCommitLock();
            fail();
        } catch (DeadTransactionException expected) {
        }


        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm);
        Transaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        tx.prepare();

        long version = ref.getVersion();
        try {
            ref.acquireCommitLock();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        try {
            ref.getLock().acquireCommitLock(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenFreeAndNotReadBefore() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.acquireCommitLock();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref, tx);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertIsCommitted(tx);
    }

    @Test
    public void whenFreeAndReadBefore() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.get();
        ref.acquireCommitLock();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref, tx);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertIsCommitted(tx);
    }

    @Test
    public void whenFreeButReadConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);
        ref.get();

        //conflicting write
        ref.atomicSet(100);
        long version = ref.getVersion();

        try {
            ref.acquireCommitLock();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenReadonlyTransaction() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.createTransactionFactoryBuilder()
                .setReadonly(true)
                .build()
                .newTransaction();
        setThreadLocalTransaction(tx);

        ref.acquireCommitLock();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref,tx);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertIsCommitted(tx);
    }

    @Test
    public void whenAlreadyPrivatizedBySelf() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.acquireCommitLock();
        ref.acquireCommitLock();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref, tx);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertIsCommitted(tx);
    }

    @Test
    @Ignore
    public void whenReadBiased() {

    }

    @Test
    public void whenAlreadyEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.acquireWriteLock();
        ref.acquireCommitLock();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref, tx);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertIsCommitted(tx);
    }

    @Test
    public void whenAlreadyPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireCommitLock(otherTx);

        try {
            ref.acquireCommitLock();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertRefHasCommitLock(ref,otherTx);
        assertSame(tx, getThreadLocalTransaction());
        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyEnsuredByOther() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.getLock().acquireWriteLock(tx);

        try {
            ref.acquireCommitLock();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertRefHasUpdateLock(ref,otherTx);
        assertSame(tx, getThreadLocalTransaction());
        assertIsAborted(tx);
    }
}

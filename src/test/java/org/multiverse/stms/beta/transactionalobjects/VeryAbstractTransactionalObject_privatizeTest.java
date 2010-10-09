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
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

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
            ref.privatize();
            fail();
        } catch (TransactionRequiredException expected) {
        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
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
            ref.privatize();
            fail();
        } catch (DeadTransactionException expected) {
        }


        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
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
            ref.privatize();
            fail();
        } catch (DeadTransactionException expected) {
        }


        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
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
            ref.privatize();
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);
        long version = ref.getVersion();

        try {
            ref.privatize(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertNull(getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenFreeAndNotReadBefore() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertIsCommitted(tx);
    }

    @Test
    public void whenFreeAndReadBefore() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.get();
        ref.privatize();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
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
            ref.privatize();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
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

        ref.privatize();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertIsCommitted(tx);
    }

    @Test
    public void whenAlreadyPrivatizedBySelf() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();
        ref.privatize();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
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

        ref.ensure();
        ref.privatize();

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsActive(tx);

        tx.commit();

        assertSurplus(0, ref);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertNull(ref.___getLockOwner());
        assertIsCommitted(tx);
    }

    @Test
    public void whenAlreadyPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.privatize();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyEnsuredByOther() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        try {
            ref.privatize();
            fail();
        } catch (ReadWriteConflict expected) {

        }

        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertIsAborted(tx);
    }
}

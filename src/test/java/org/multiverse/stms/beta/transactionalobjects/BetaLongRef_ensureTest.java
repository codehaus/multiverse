package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_ensureTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenSuccess() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure(tx);

        assertUpdateBiased(ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNoTransaction_thenNoTransactionFoundException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.ensure();
            fail();
        } catch (NoTransactionFoundException expected) {
        }

        assertUpdateBiased(ref);
        assertNull(getThreadLocalTransaction());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNullTransactionProvided_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.ensure(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertUpdateBiased(ref);
        assertNull(getThreadLocalTransaction());
        assertUpdateBiased(ref);
        assertSurplus(0, ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenCommittedTransactionProvided_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();

        try {
            ref.ensure(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertUpdateBiased(ref);
        assertIsCommitted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenAbortedTransactionProvided_thenDeadTransactionException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();

        try {
            ref.ensure(tx);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertUpdateBiased(ref);
        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();

        try {
            ref.ensure(tx);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertUpdateBiased(ref);
        assertIsAborted(tx);
        assertSame(committed, ref.___unsafeLoad());
        assertHasNoUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
    }

    @Test
    public void whenAlreadyEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure();
        ref.ensure();

        assertUpdateBiased(ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAlreadyPrivatizedBySelf() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize(tx);
        ref.ensure(tx);

        assertUpdateBiased(ref);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    @Ignore
    public void whenReadBiased() {

    }

    @Test
    public void whenAlreadyEnsuredByOther() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        try {
            ref.ensure(tx);
            fail();
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasUpdateLock(ref);
        assertSurplus(1, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenAlreadyPrivatizedByOther() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.ensure(tx);
            fail();
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertUpdateBiased(ref);
        assertHasCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(1, ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenReadConflict() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.get(tx);
        ref.atomicSet(100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        try {
            ref.ensure(tx);
            fail();
        } catch (ReadConflict expected) {

        }

        assertIsAborted(tx);
        assertUpdateBiased(ref);
        assertHasNoCommitLock(ref);
        assertHasNoUpdateLock(ref);
        assertSurplus(0, ref);
        assertNull(ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenNormalTransactionUsed() {
        BetaLongRef ref = newLongRef(stm);
        LongRefTranlocal committed = ref.___unsafeLoad();
        Transaction tx = stm.startDefaultTransaction();

        ref.ensure(tx);

        assertUpdateBiased(ref);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertSame(tx, ref.___getLockOwner());
        assertSame(committed, ref.___unsafeLoad());
    }
}

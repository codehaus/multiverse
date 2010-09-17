package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_get0Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        LongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.get();
            fail();
        } catch (PreparedTransactionException expected) {

        }
    }

    @Test
    public void whenPrivatizedBySelf_thenSuccess() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();
        long value = ref.get();

        assertEquals(100, value);
        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.ensure();
        long value = ref.get();

        assertEquals(100, value);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.get();
            fail();
        } catch (ReadConflict expected) {
        }


        assertHasUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenEnsuredByother_thenReadStillPossible() {
        BetaLongRef ref = newLongRef(stm, 100);
        LongRefTranlocal committed = ref.___unsafeLoad();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        long value = ref.get();

        assertEquals(100, value);
        assertHasUpdateLock(ref);
        assertHasNoCommitLock(ref);
        assertSurplus(2, ref);
        assertUpdateBiased(ref);
        assertSame(otherTx, ref.___getLockOwner());
        assertIsActive(otherTx);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSame(committed, ref.___unsafeLoad());
    }

    @Test
    public void whenActiveTransactionAvailable_thenPreparedTransactionException() {
        LongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        long value = ref.get();

        assertEquals(10, value);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenNoTransactionAvailable_thenExecutedAtomically() {
        BetaLongRef ref = newLongRef(stm, 10);

        long result = ref.get();

        assertNull(getThreadLocalTransaction());
        assertEquals(10, result);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenExecutedAtomically() {
        LongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        long value = ref.get();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenAbortedTransactionAvailable_thenExecutedAtomically() {
        LongRef ref = newLongRef(stm, 10);

        BetaTransaction tx = stm.startDefaultTransaction();
        tx.abort();
        setThreadLocalTransaction(tx);

        long value = ref.get();

        assertEquals(10, value);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
    }
}

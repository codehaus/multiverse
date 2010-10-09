package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertVersionAndValue;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;
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
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        ref.privatize();

        long value = ref.get();

        assertEquals(100, value);
        assertHasNoUpdateLock(ref);
        assertHasCommitLock(ref);
        assertSurplus(1, ref);
        assertUpdateBiased(ref);
        assertSame(tx, ref.___getLockOwner());
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(version, ref.getVersion());
        assertEquals(100, ref.___weakRead());
    }

    @Test
    public void whenEnsuredBySelf() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

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
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenPrivatizedByOther_thenReadConflict() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

        BetaTransaction tx = stm.startDefaultTransaction();
        setThreadLocalTransaction(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        try {
            ref.get();
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
    public void whenEnsuredByother_thenReadStillPossible() {
        BetaLongRef ref = newLongRef(stm, 100);
        long version = ref.getVersion();

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
        assertVersionAndValue(ref, version, 100);
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
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        long initialValue = 10;
        BetaLongRef ref = newLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.get();
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertNull(getThreadLocalTransaction());
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
            ref.get();
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
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
            ref.get();
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}

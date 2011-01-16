package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.stms.gamma.ArrayGammaTransactionFactory;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.MapGammaTransactionFactory;
import org.multiverse.stms.gamma.MonoGammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

@RunWith(Parameterized.class)
public class GammaLongRef_set1Test {

    private final GammaTransactionFactory transactionFactory;
    private final GammaStm stm;

    public GammaLongRef_set1Test(GammaTransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
        this.stm = transactionFactory.getTransactionConfiguration().getStm();
    }

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Parameterized.Parameters
    public static Collection<TransactionFactory[]> configs() {
        return asList(
                new TransactionFactory[]{new MapGammaTransactionFactory(new GammaStm())},
                new TransactionFactory[]{new ArrayGammaTransactionFactory(new GammaStm())},
                new TransactionFactory[]{new MonoGammaTransactionFactory(new GammaStm())}
        );
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.prepare();
        setThreadLocalTransaction(tx);

        try {
            ref.set(30);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertIsAborted(tx);
        assertEquals(10, ref.atomicGet());
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenActiveTransactionAvailable_thenPreparedTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);
        long value = ref.set(20);

        assertIsActive(tx);
        assertEquals(20, value);
        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, version, 10);

        tx.commit();

        assertIsCommitted(tx);
        assertEquals(20, ref.atomicGet());
        assertSame(tx, getThreadLocalTransaction());
        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertUpdateBiased(ref);
    }

    @Test
    public void whenLocked_thenReadWriteConflict() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);
        try {
            ref.set(20);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);

        otherTx.abort();
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 0);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
    }

    @Test
    public void whenNoChange() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        long result = ref.set(initialValue);

        tx.commit();

        assertIsCommitted(tx);
        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNoTransactionFound_thenNoTransactionFoundException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.set(20);
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertRefHasNoLocks(ref);
        assertSurplus(ref, 0);
        assertUpdateBiased(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPrivatizedBySelf_thenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        ref.getLock().acquire(LockMode.Commit);
        long value = ref.set(200);

        assertEquals(200, value);
        assertRefHasCommitLock(ref, tx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenEnsuredBySelf_thenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        ref.getLock().acquire(LockMode.Write);
        long value = ref.set(200);

        assertEquals(200, value);
        assertRefHasWriteLock(ref, tx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenPrivatizedByOtherAndFirstTimeRead_thenReadConflict() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Commit);

        try {
            ref.set(200);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasCommitLock(ref, otherTx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(otherTx);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenEnsuredByOther_thenSetPossibleButCommitFails() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        long value = ref.set(200);
        assertEquals(200, value);
        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(otherTx);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict e) {

        }

        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(otherTx);
        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenExecutedAtomically() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
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
        assertSurplus(ref, 0);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.commit();
        setThreadLocalTransaction(tx);

        try {
            ref.set(20);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
        assertSurplus(ref, 0);
        assertUpdateBiased(ref);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
    }
}

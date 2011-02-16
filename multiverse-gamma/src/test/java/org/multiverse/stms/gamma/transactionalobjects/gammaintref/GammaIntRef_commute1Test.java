package org.multiverse.stms.gamma.transactionalobjects.gammaintref;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.api.exceptions.TransactionRequiredException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.IntFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaIntRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRefTranlocal;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.fat.FatFixedLengthGammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTransactionFactory;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.*;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

@RunWith(Parameterized.class)
public class GammaIntRef_commute1Test {
    private final GammaTransactionFactory transactionFactory;
    private final GammaStm stm;

    public GammaIntRef_commute1Test(GammaTransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
        this.stm = transactionFactory.getConfiguration().getStm();
    }

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Parameterized.Parameters
    public static Collection<TransactionFactory[]> configs() {
        return asList(
                new TransactionFactory[]{new FatVariableLengthGammaTransactionFactory(new GammaStm())},
                new TransactionFactory[]{new FatFixedLengthGammaTransactionFactory(new GammaStm())},
                new TransactionFactory[]{new FatMonoGammaTransactionFactory(new GammaStm())}
        );
    }

    @Test
    public void whenActiveTransactionAvailable() {
        GammaIntRef ref = new GammaIntRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);
        IntFunction function = Functions.incIntFunction(1);
        ref.commute(function);

        GammaRefTranlocal commuting = tx.getRefTranlocal(ref);
        assertNotNull(commuting);
        assertTrue(commuting.isCommuting());
        assertFalse(commuting.isRead());
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertEquals(0, commuting.long_value);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        tx.commit();

        assertEquals(1, ref.atomicGet());
        assertIsCommitted(tx);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertWriteBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailableAndNoChange() {
        GammaIntRef ref = new GammaIntRef(stm);
        long version = ref.getVersion();
        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);
        IntFunction function = Functions.identityIntFunction();
        ref.commute(function);

        GammaRefTranlocal commuting = tx.getRefTranlocal(ref);
        assertNotNull(commuting);
        assertTrue(commuting.isCommuting());
        assertFalse(commuting.isRead());
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertEquals(0, commuting.long_value);
        assertIsActive(tx);
        assertSame(tx, getThreadLocalTransaction());
        tx.commit();

        assertEquals(0, ref.atomicGet());
        assertVersionAndValue(ref, version, 0);
        assertIsCommitted(tx);
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertWriteBiased(ref);
    }

    @Test
    public void whenActiveTransactionAvailableAndNullFunction_thenNullPointerException() {
        GammaIntRef ref = new GammaIntRef(stm);
        long version = ref.getVersion();
        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        try {
            ref.commute(null);
            fail();
        } catch (NullPointerException expected) {
        }


        assertIsAborted(tx);
        assertSurplus(ref, 0);
        assertWriteBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 0);
    }

    @Test
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        int initialValue = 10;
        GammaIntRef ref = new GammaIntRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        IntFunction function = Functions.incIntFunction(1);
        try {
            ref.commute(function);
            fail();
        } catch (TransactionRequiredException expected) {

        }

        assertSurplus(ref, 0);
        assertWriteBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
        int initialValue = 10;
        GammaIntRef ref = new GammaIntRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);
        tx.commit();

        IntFunction function = Functions.incIntFunction(1);
        try {
            ref.commute(function);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(ref, 0);
        assertWriteBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        int initialValue = 10;
        GammaIntRef ref = new GammaIntRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);
        tx.abort();

        IntFunction function = Functions.incIntFunction(1);
        try {
            ref.commute(function);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(ref, 0);
        assertWriteBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        GammaIntRef ref = new GammaIntRef(stm, 2);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);
        tx.prepare();

        IntFunction function = Functions.incIntFunction(1);
        try {
            ref.commute(function);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertSurplus(ref, 0);
        assertWriteBiased(ref);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, version, 2);
        assertEquals(2, ref.atomicGet());
    }

    @Test
    public void whenAlreadyEnsuredBySelf_thenNoCommute() {
        GammaIntRef ref = new GammaIntRef(stm, 2);

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        ref.getLock().acquire(LockMode.Write);
        IntFunction function = Functions.incIntFunction(1);
        ref.commute(function);

        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);
        assertNotNull(tranlocal);
        assertFalse(tranlocal.isCommuting());
        assertEquals(3, tranlocal.long_value);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, tx);
        assertSurplus(ref, 1);
        assertWriteBiased(ref);

        tx.commit();

        assertSurplus(ref, 0);
        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(3, ref.atomicGet());
    }

    @Test
    public void whenAlreadyPrivatizedBySelf_thenNoCommute() {
        GammaIntRef ref = new GammaIntRef(stm, 2);

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        ref.getLock().acquire(LockMode.Exclusive);
        IntFunction function = Functions.incIntFunction(1);
        ref.commute(function);

        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);
        assertNotNull(tranlocal);
        assertFalse(tranlocal.isCommuting());
        assertEquals(3, tranlocal.long_value);
        assertIsActive(tx);
        assertRefHasExclusiveLock(ref, tx);
        assertSurplus(ref, 1);
        assertWriteBiased(ref);

        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
        assertSame(tx, getThreadLocalTransaction());
        assertEquals(3, ref.atomicGet());
        assertSurplus(ref, 0);
    }

    @Test
    @Ignore
    public void whenReadLockAcquiredByOther() {

    }

    @Test
    public void whenWriteLockAcquiredByOther_thenCommuteSucceedsButCommitFails() {
        GammaIntRef ref = new GammaIntRef(stm, 2);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        IntFunction function = Functions.incIntFunction(1);
        ref.commute(function);

        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);
        assertNotNull(tranlocal);
        assertTrue(tranlocal.isCommuting());
        assertHasCommutingFunctions(tranlocal, function);
        assertIsActive(tx);
        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(ref, 1);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertRefHasWriteLock(ref, otherTx);
        assertVersionAndValue(ref, version, 2);
        assertSurplus(ref, 1);
    }

    @Test
    public void whenExclusiveLockByOther_thenCommuteSucceedsButCommitFails() {
        GammaIntRef ref = new GammaIntRef(stm, 2);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);

        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        IntFunction function = Functions.incIntFunction(1);
        ref.commute(function);

        GammaRefTranlocal tranlocal = tx.getRefTranlocal(ref);
        assertNotNull(tranlocal);
        assertTrue(tranlocal.isCommuting());
        assertHasCommutingFunctions(tranlocal, function);
        assertIsActive(tx);
        assertRefHasExclusiveLock(ref, otherTx);
        assertSurplus(ref, 1);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTransaction());
        assertRefHasExclusiveLock(ref, otherTx);
        assertVersionAndValue(ref, version, 2);
        assertSurplus(ref, 1);
    }
}

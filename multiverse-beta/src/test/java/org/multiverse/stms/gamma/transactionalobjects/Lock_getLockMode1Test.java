package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
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
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

@RunWith(Parameterized.class)
public class Lock_getLockMode1Test {

    private final GammaTransactionFactory transactionFactory;
    private final GammaStm stm;

    public Lock_getLockMode1Test(GammaTransactionFactory transactionFactory) {
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
                new TransactionFactory[]{new MapGammaTransactionFactory(new GammaStm())},
                new TransactionFactory[]{new ArrayGammaTransactionFactory(new GammaStm())},
                new TransactionFactory[]{new MonoGammaTransactionFactory(new GammaStm())}
        );
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTransaction() {
        GammaLongRef ref = new GammaLongRef(stm);

        ref.getLock().getLockMode(null);
    }

    @Test
    public void other_whenLockedForReadByOther() {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Read);

        GammaTransaction tx = transactionFactory.newTransaction();
        LockMode result = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, result);
    }

    @Test
    public void other_whenLockedForWriteByOther() {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Write);

        GammaTransaction tx = transactionFactory.newTransaction();
        LockMode result = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, result);
    }

    @Test
    public void other_whenLockForCommitByOther() {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        GammaTransaction tx = transactionFactory.newTransaction();
        LockMode result = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, result);
    }

    @Test
    public void self_whenNotReadBySelf() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, lockMode);
        assertIsActive(tx);
        assertNull(tx.locate(ref));
    }

    @Test
    public void self_whenNotLockedBySelf() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        ref.get(tx);
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.None, lockMode);
        assertIsActive(tx);
    }

    @Test
    public void self_whenReadLockedByself() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        ref.getLock().acquire(tx, LockMode.Read);
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.Read, lockMode);
        assertIsActive(tx);
    }

    @Test
    public void self_whenWriteLockedBySelf() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        ref.getLock().acquire(tx, LockMode.Write);
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.Write, lockMode);
        assertIsActive(tx);
    }

    @Test
    public void self_whenExclusiveLockedBySelf() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        ref.getLock().acquire(tx, LockMode.Exclusive);
        LockMode lockMode = ref.getLock().getLockMode(tx);

        assertEquals(LockMode.Exclusive, lockMode);
        assertIsActive(tx);
    }

    // ====================== states ==================================

    @Test
    public void whenTransactionPrepared_thenPreparedTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.prepare();

        try {
            ref.getLock().getLockMode(tx);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.abort();

        try {
            ref.getLock().getLockMode(tx);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.commit();

        try {
            ref.getLock().getLockMode(tx);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
    }
}

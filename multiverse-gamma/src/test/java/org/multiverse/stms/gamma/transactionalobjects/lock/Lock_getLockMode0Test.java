package org.multiverse.stms.gamma.transactionalobjects.lock;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.multiverse.api.LockMode;
import org.multiverse.api.TransactionFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.fat.FatFixedLengthGammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTransactionFactory;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasLockMode;

@RunWith(Parameterized.class)
public class Lock_getLockMode0Test {


    private final GammaTransactionFactory transactionFactory;
    private final GammaStm stm;

    public Lock_getLockMode0Test(GammaTransactionFactory transactionFactory) {
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

    @Test(expected = NullPointerException.class)
    public void whenNullTransaction() {
        GammaLongRef ref = new GammaLongRef(stm);

        ref.getLock().getLockMode(null);
    }

    @Test
    public void other_whenLockedForWriteByOther() {
        whenLockedByOther(LockMode.Read);
        whenLockedByOther(LockMode.Write);
        whenLockedByOther(LockMode.Exclusive);
    }

    public void whenLockedByOther(LockMode lockMode) {
        GammaLongRef ref = new GammaLongRef(stm);
        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, lockMode);

        GammaTransaction tx = transactionFactory.newTransaction();
        setThreadLocalTransaction(tx);
        LockMode result = ref.getLock().getLockMode();

        assertEquals(LockMode.None, result);
        assertRefHasLockMode(ref,otherTx, lockMode.asInt());
    }

    @Test
    public void whenLockedBySelf(){
        self_whenLocked(LockMode.None);
        self_whenLocked(LockMode.Read);
        self_whenLocked(LockMode.Write);
        self_whenLocked(LockMode.Exclusive);
    }

    public void self_whenLocked(LockMode lockMode){
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        ref.getLock().acquire(tx, lockMode);
        setThreadLocalTransaction(tx);
        LockMode result = ref.getLock().getLockMode();

        assertEquals(lockMode, result);
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

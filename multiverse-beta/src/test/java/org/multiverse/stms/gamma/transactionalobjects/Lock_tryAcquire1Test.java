package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
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
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertLockMode;
import static org.multiverse.stms.gamma.GammaTestUtils.assertRefHasNoLocks;

/**
 * @author Peter Veentjer
 */
@RunWith(Parameterized.class)
public class Lock_tryAcquire1Test {

    private final GammaTransactionFactory transactionFactory;
    private final GammaStm stm;

    public Lock_tryAcquire1Test(GammaTransactionFactory transactionFactory) {
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
    @Ignore
    public void test() {

    }

    @Test
    public void whenLockAvailableButConflictFound() {

    }

    @Test
    @Ignore
    public void whenLockedByOther() {
        whenLockedByOther(LockMode.None, LockMode.None, true, LockMode.None);
        whenLockedByOther(LockMode.None, LockMode.Read, true, LockMode.Read);
        whenLockedByOther(LockMode.None, LockMode.Write, true, LockMode.Write);
        whenLockedByOther(LockMode.None, LockMode.Commit, true, LockMode.Commit);

        whenLockedByOther(LockMode.Read, LockMode.None, true, LockMode.None);
        whenLockedByOther(LockMode.Read, LockMode.Read, true, LockMode.Read);
        whenLockedByOther(LockMode.Read, LockMode.Write, false, LockMode.Read);
        whenLockedByOther(LockMode.Read, LockMode.Commit, false, LockMode.Read);

        whenLockedByOther(LockMode.Write, LockMode.None, false, LockMode.Write);
        whenLockedByOther(LockMode.Write, LockMode.Read, false, LockMode.Write);
        whenLockedByOther(LockMode.Write, LockMode.Write, false, LockMode.Write);
        whenLockedByOther(LockMode.Write, LockMode.Commit, false, LockMode.Write);

        whenLockedByOther(LockMode.Commit, LockMode.None, false, LockMode.Commit);
        whenLockedByOther(LockMode.Commit, LockMode.Read, false, LockMode.Commit);
        whenLockedByOther(LockMode.Commit, LockMode.Write, false, LockMode.Commit);
        whenLockedByOther(LockMode.Commit, LockMode.Commit, false, LockMode.Commit);
    }

    public void whenLockedByOther(LockMode otherLockMode, LockMode selfLockMode, boolean success, LockMode expectedLockMode) {

    }

    @Test
    public void whenNotLockedByOther() {
        whenNotLockedByOther(LockMode.None, LockMode.None, LockMode.None);
        whenNotLockedByOther(LockMode.None, LockMode.Read, LockMode.Read);
        whenNotLockedByOther(LockMode.None, LockMode.Write, LockMode.Write);
        whenNotLockedByOther(LockMode.None, LockMode.Commit, LockMode.Commit);

        whenNotLockedByOther(LockMode.Read, LockMode.None, LockMode.Read);
        whenNotLockedByOther(LockMode.Read, LockMode.Read, LockMode.Read);
        whenNotLockedByOther(LockMode.Read, LockMode.Write, LockMode.Write);
        whenNotLockedByOther(LockMode.Read, LockMode.Commit, LockMode.Commit);

        whenNotLockedByOther(LockMode.Write, LockMode.None, LockMode.Write);
        whenNotLockedByOther(LockMode.Write, LockMode.Read, LockMode.Write);
        whenNotLockedByOther(LockMode.Write, LockMode.Write, LockMode.Write);
        whenNotLockedByOther(LockMode.Write, LockMode.Commit, LockMode.Commit);

        whenNotLockedByOther(LockMode.Commit, LockMode.None, LockMode.Commit);
        whenNotLockedByOther(LockMode.Commit, LockMode.Read, LockMode.Commit);
        whenNotLockedByOther(LockMode.Commit, LockMode.Write, LockMode.Commit);
        whenNotLockedByOther(LockMode.Commit, LockMode.Commit, LockMode.Commit);
    }

    public void whenNotLockedByOther(LockMode initialLockMode, LockMode desiredLockMode, LockMode expectedLockMode) {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        GammaRefTranlocal tranlocal = ref.openForRead(tx, initialLockMode.asInt());
        boolean result = ref.getLock().tryAcquire(tx, desiredLockMode);

        assertTrue(result);
        assertLockMode(ref, expectedLockMode);
        assertEquals(expectedLockMode.asInt(), tranlocal.getLockMode());
    }

    @Test
    public void whenNullTransaction() {
        GammaLongRef ref = new GammaLongRef(stm);

        try {
            ref.getLock().tryAcquire(null, LockMode.None);
            fail();
        } catch (NullPointerException expected) {
        }

        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenNullLockMode_thenTransactionAborted() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        try {
            ref.getLock().tryAcquire(tx, null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionPrepared() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.prepare();
        try {
            ref.getLock().tryAcquire(tx, LockMode.Read);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionAborted() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.abort();
        try {
            ref.getLock().tryAcquire(tx, LockMode.Read);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenTransactionCommitted() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.commit();
        try {
            ref.getLock().tryAcquire(tx, LockMode.Read);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }
}

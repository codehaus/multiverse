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
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.gamma.ArrayGammaTransactionFactory;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.MapGammaTransactionFactory;
import org.multiverse.stms.gamma.MonoGammaTransactionFactory;
import org.multiverse.stms.gamma.transactions.GammaTransaction;
import org.multiverse.stms.gamma.transactions.GammaTransactionFactory;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

@RunWith(Parameterized.class)
public class GammaLongRef_get1Test {

    private final GammaTransactionFactory transactionFactory;
    private final GammaStm stm;

    public GammaLongRef_get1Test(GammaTransactionFactory transactionFactory) {
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

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        LongRef ref = new GammaLongRef(stm, 10);

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.prepare();

        try {
            ref.get(tx);
            fail();
        } catch (PreparedTransactionException expected) {

        }
    }

    @Test
    public void whenLockedForCommitBySelf_thenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();

        ref.getLock().acquire(tx, LockMode.Exclusive);

        long value = ref.get(tx);

        assertEquals(100, value);
        assertRefHasExclusiveLock(ref, tx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertEquals(version, ref.getVersion());
        assertEquals(100, ref.long_value);
    }

    @Test
    public void whenLockedForWriteBySelf() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();

        ref.getLock().acquire(tx, LockMode.Write);
        long value = ref.get(tx);

        assertEquals(100, value);
        assertRefHasWriteLock(ref, tx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenLockedForReadBySelf() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();

        ref.getLock().acquire(tx, LockMode.Read);
        long value = ref.get(tx);

        assertEquals(100, value);
        assertRefHasReadLock(ref, tx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(tx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenCommtLockAcquiredByOther_thenReadConflict() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();

        GammaTransaction otherTx = transactionFactory.newTransaction();
        ref.getLock().acquire(otherTx, LockMode.Exclusive);

        try {
            ref.get(tx);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertRefHasExclusiveLock(ref, otherTx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(otherTx);
        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenEnsuredByother_thenReadStillPossible() {
        GammaLongRef ref = new GammaLongRef(stm, 100);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();

        GammaTransaction otherTx = transactionFactory.newTransaction();

        ref.getLock().acquire(otherTx, LockMode.Write);

        long value = ref.get(tx);

        assertEquals(100, value);
        assertRefHasWriteLock(ref, otherTx);
        assertSurplus(ref, 1);
        assertUpdateBiased(ref);
        assertIsActive(otherTx);
        assertIsActive(tx);
        assertVersionAndValue(ref, version, 100);
    }

    @Test
    public void whenActiveTransactionAvailable_thenPreparedTransactionException() {
        LongRef ref = new GammaLongRef(stm, 10);

        GammaTransaction tx = transactionFactory.newTransaction();

        long value = ref.get(tx);

        assertEquals(10, value);
        assertIsActive(tx);
    }

    @Test
    public void whenNullTransactionAvailable_thenNullPointerException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        try {
            ref.get(null);
            fail();
        } catch (NullPointerException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.commit();

        try {
            ref.get(tx);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertIsCommitted(tx);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.abort();

        try {
            ref.get(tx);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }
}

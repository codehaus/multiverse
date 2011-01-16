package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
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
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

@RunWith(Parameterized.class)
public class GammaLongRef_getAndSet2Test {
   private final GammaTransactionFactory transactionFactory;
    private final GammaStm stm;

    public GammaLongRef_getAndSet2Test(GammaTransactionFactory transactionFactory) {
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
    public void whenNullTransaction() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        try {
            ref.getAndSet(null, 11);
            fail();
        } catch (NullPointerException expected) {
        }

        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenPreparedTransaction_thenPreparedTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        tx.prepare();

        try {
            ref.getAndSet(tx, 11);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenAbortedTransaction_thenDeadTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();
        GammaTransaction tx = transactionFactory.newTransaction();
        tx.abort();

        try {
            ref.getAndSet(tx, 11);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenCommittedTransaction_thenCommittedTransactionException() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();
        GammaTransaction tx = transactionFactory.newTransaction();
        tx.commit();

        try {
            ref.getAndSet(tx, 11);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenSuccess() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        long result = ref.getAndSet(tx, 20);
        tx.commit();

        assertEquals(10, result);
        assertVersionAndValue(ref, version + 1, 20);
    }

    @Test
    public void whenNormalTransactionUsed() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        long result = ref.getAndSet(tx, 20);
        tx.commit();

        assertEquals(10, result);
        assertVersionAndValue(ref, version + 1, 20);
    }

    @Test
    public void whenNoChange() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = transactionFactory.newTransaction();
        long value = ref.getAndSet(tx, 10);
        tx.commit();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertEquals(10, ref.atomicGet());
        assertNull(getThreadLocalTransaction());
        assertSurplus(ref, 0);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    public void whenListenersAvailable() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        long newValue = 20;
        LongRefAwaitThread thread = new LongRefAwaitThread(ref, newValue);
        thread.start();

        sleepMs(500);

        GammaTransaction tx = transactionFactory.newTransaction();
        long result = ref.getAndSet(tx, newValue);
        tx.commit();

        joinAll(thread);

        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, newValue);
    }
}

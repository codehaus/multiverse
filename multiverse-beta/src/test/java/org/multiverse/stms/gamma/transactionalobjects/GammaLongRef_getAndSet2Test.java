package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.stms.gamma.GammaTestUtils.assertSurplus;
import static org.multiverse.stms.gamma.GammaTestUtils.assertVersionAndValue;

public class GammaLongRef_getAndSet2Test {
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
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

        GammaTransaction tx = stm.startDefaultTransaction();
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
        GammaTransaction tx = stm.startDefaultTransaction();
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
        GammaTransaction tx = stm.startDefaultTransaction();
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

        GammaTransaction tx = stm.startDefaultTransaction();
        long result = ref.getAndSet(tx, 20);
        tx.commit();

        assertEquals(10, result);
        assertVersionAndValue(ref, version + 1, 20);
    }

    @Test
    public void whenNormalTransactionUsed() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        long result = ref.getAndSet(tx, 20);
        tx.commit();

        assertEquals(10, result);
        assertVersionAndValue(ref, version + 1, 20);
    }

    @Test
    public void whenNoChange() {
        GammaLongRef ref = new GammaLongRef(stm, 10);
        long version = ref.getVersion();

        GammaTransaction tx = stm.startDefaultTransaction();
        long value = ref.getAndSet(tx, 10);
        tx.commit();

        assertEquals(10, value);
        assertIsCommitted(tx);
        assertEquals(10, ref.atomicGet());
        assertNull(getThreadLocalTransaction());
        assertSurplus(0, ref);
        assertVersionAndValue(ref, version, 10);
    }

    @Test
    @Ignore
    public void whenListenersAvailable() {
        /*
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        long newValue = 20;
        LongRefAwaitThread thread = new LongRefAwaitThread(ref, newValue);
        thread.start();

        sleepMs(500);

        GammaTransaction tx = stm.startDefaultTransaction();
        long result = ref.getAndSet(tx, newValue);
        tx.commit();

        joinAll(thread);

        assertEquals(initialValue, result);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion + 1, newValue);   */
    }
}

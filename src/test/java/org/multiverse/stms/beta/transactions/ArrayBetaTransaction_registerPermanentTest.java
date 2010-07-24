package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;

public class ArrayBetaTransaction_registerPermanentTest {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }
       
    @Test
    public void whenNullListener_thenNullPointerException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);

        try {
            tx.registerPermanent(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertActive(tx);
    }

    @Test
    @Ignore
    public void whenNew() {

    }

    @Test
    public void whenFirst() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        //todo: tx.start()

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(listener);

        assertActive(tx);
        verifyZeroInteractions(listener);
        assertSame(listener, getField(tx, "permanentListeners"));
        assertSame(null, getField(tx, "normalListeners"));
    }

    @Test
    public void whenTwoRegisters_thenTransformedToList() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        //todo: tx.start()

        TransactionLifecycleListener listener1 = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener listener2 = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(listener1);
        tx.registerPermanent(listener2);

        assertActive(tx);
        verifyZeroInteractions(listener1);
        assertEquals(asList(listener1, listener2), getField(tx, "permanentListeners"));
        assertSame(null, getField(tx, "normalListeners"));
    }

    @Test
    public void whenPrepared() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.prepare(pool);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        tx.registerPermanent(listener);

        assertPrepared(tx);
        verifyZeroInteractions(listener);
        assertSame(listener, getField(tx, "permanentListeners"));
        assertNull(getField(tx, "normalListeners"));
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.abort();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.registerPermanent(listener);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertAborted(tx);
        assertNull(getField(tx, "permanentListeners"));
        assertNull(getField(tx, "normalListeners"));
        verifyZeroInteractions(listener);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ArrayBetaTransaction tx = new ArrayBetaTransaction(stm, 10);
        tx.commit();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.registerPermanent(listener);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertNull(getField(tx, "permanentListeners"));
        assertNull(getField(tx, "normalListeners"));
        verifyZeroInteractions(listener);
    }

}

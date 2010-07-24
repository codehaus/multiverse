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

public class ArrayTreeBetaTransaction_registerTest {

    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }

    @Test
    public void whenNullListener_thenNullPointerException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);

        try {
            tx.register(null);
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
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        //todo: tx.start()

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        assertActive(tx);
        verifyZeroInteractions(listener);
        assertSame(listener, getField(tx, "normalListeners"));
        assertSame(null, getField(tx, "permanentListeners"));
    }

    @Test
    public void whenTwoRegisters_thenTransformedToList() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        //todo: tx.start()

        TransactionLifecycleListener listener1 = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener listener2 = mock(TransactionLifecycleListener.class);
        tx.register(listener1);
        tx.register(listener2);

        assertActive(tx);
        verifyZeroInteractions(listener1);
        assertEquals(asList(listener1, listener2), getField(tx, "normalListeners"));
        assertSame(null, getField(tx, "permanentListeners"));
    }

    @Test
    public void whenPrepared() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.prepare(pool);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        tx.register(listener);

        assertPrepared(tx);
        verifyZeroInteractions(listener);
        assertSame(listener, getField(tx, "normalListeners"));
        assertNull(getField(tx, "permanentListeners"));
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.abort();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.register(listener);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertAborted(tx);
        assertNull(getField(tx, "normalListeners"));
        assertNull(getField(tx, "permanentListeners"));
        verifyZeroInteractions(listener);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        ArrayTreeBetaTransaction tx = new ArrayTreeBetaTransaction(stm);
        tx.commit();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.register(listener);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertNull(getField(tx, "normalListeners"));
        assertNull(getField(tx, "permanentListeners"));
        verifyZeroInteractions(listener);
    }
}

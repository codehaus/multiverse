package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;

public class FatArrayBetaTransaction_registerTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenNullListener_thenNullPointerException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        try {
            tx.register(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertAborted(tx);
    }

    @Test
    @Ignore
    public void whenNew() {

    }

    @Test
    public void whenFirst() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        //todo: tx.start()

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        assertActive(tx);
        verifyZeroInteractions(listener);
        assertHasNormalListeners(tx, listener);
        assertHasNoPermanentListeners(tx);
    }

    @Test
    public void whenTwoRegisters_thenTransformedToList() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        //todo: tx.start()

        TransactionLifecycleListener listener1 = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener listener2 = mock(TransactionLifecycleListener.class);
        tx.register(listener1);
        tx.register(listener2);

        assertActive(tx);
        verifyZeroInteractions(listener1);
        assertHasNormalListeners(tx, listener1, listener2);
        assertHasNoPermanentListeners(tx);
    }

    @Test
    public void whenPrepared() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare(pool);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        tx.register(listener);

        assertPrepared(tx);
        verifyZeroInteractions(listener);
        assertHasNoPermanentListeners(tx);
        assertHasNormalListeners(tx, listener);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.register(listener);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertAborted(tx);
        assertHasNoPermanentListeners(tx);
        assertHasNoNormalListeners(tx);
        verifyZeroInteractions(listener);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.register(listener);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertHasNoPermanentListeners(tx);
        assertHasNoNormalListeners(tx);
        verifyZeroInteractions(listener);
    }
}
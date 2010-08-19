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

public class FatArrayBetaTransaction_registerPermanentTest {
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
            tx.registerPermanent(pool,null);
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

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(pool,listener);

        assertActive(tx);
        verifyZeroInteractions(listener);
        assertHasPermanentListeners(tx, listener);
        assertHasNoNormalListeners(tx);
    }

    @Test
    public void whenTwoRegisters_thenTransformedToList() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);

        TransactionLifecycleListener listener1 = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener listener2 = mock(TransactionLifecycleListener.class);
        tx.registerPermanent(pool,listener1);
        tx.registerPermanent(pool,listener2);

        assertActive(tx);
        verifyZeroInteractions(listener1);
        assertHasPermanentListeners(tx, listener1, listener2);
        assertHasNoNormalListeners(tx);
    }

    @Test
    public void whenPrepared() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.prepare(pool);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        tx.registerPermanent(pool,listener);

        assertPrepared(tx);
        verifyZeroInteractions(listener);
        assertHasPermanentListeners(tx, listener);
        assertHasNoNormalListeners(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.abort();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.registerPermanent(pool,listener);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertAborted(tx);
        assertHasNoNormalListeners(tx);
        assertHasNoPermanentListeners(tx);
        verifyZeroInteractions(listener);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        FatArrayBetaTransaction tx = new FatArrayBetaTransaction(stm);
        tx.commit();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.registerPermanent(pool,listener);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertCommitted(tx);
        assertHasNoNormalListeners(tx);
        assertHasNoPermanentListeners(tx);
        verifyZeroInteractions(listener);
    }

}

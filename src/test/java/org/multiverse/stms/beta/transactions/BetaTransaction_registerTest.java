package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.multiverse.TestUtils.*;

public abstract class BetaTransaction_registerTest implements BetaStmConstants {

    protected BetaStm stm;

    public abstract BetaTransaction newTransaction();

    public abstract BetaTransaction newTransaction(BetaTransactionConfiguration config);


    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenNullListener_thenNullPointerException() {
        BetaTransaction tx = newTransaction();

        try {
            tx.register(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenFirst() {
        BetaTransaction tx = newTransaction();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        assertIsActive(tx);
        verifyZeroInteractions(listener);
        assertHasNormalListeners(tx, listener);
    }

    @Test
    public void whenMultipleRegisters() {
        BetaTransaction tx = newTransaction();

        TransactionLifecycleListener listener1 = mock(TransactionLifecycleListener.class);
        TransactionLifecycleListener listener2 = mock(TransactionLifecycleListener.class);
        tx.register(listener1);
        tx.register(listener2);

        assertIsActive(tx);
        verifyZeroInteractions(listener1);
        assertHasNormalListeners(tx, listener1, listener2);
    }

    @Test
    public void whenPrepared() {
        BetaTransaction tx = newTransaction();
        tx.prepare();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        tx.register(listener);

        assertIsPrepared(tx);
        verifyZeroInteractions(listener);
        assertHasNormalListeners(tx, listener);
    }

    @Test
    @Ignore
    public void whenUndefined() {

    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.abort();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.register(listener);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsAborted(tx);
        assertHasNoNormalListeners(tx);
        verifyZeroInteractions(listener);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        BetaTransaction tx = newTransaction();
        tx.commit();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        try {
            tx.register(listener);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertHasNoNormalListeners(tx);
        verifyZeroInteractions(listener);
    }
}

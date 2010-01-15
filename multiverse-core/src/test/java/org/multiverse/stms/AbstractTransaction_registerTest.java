package org.multiverse.stms;

import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionLifecycleEvent;
import org.multiverse.api.TransactionLifecycleListener;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsActive;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_registerTest {

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.prepare();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        try {
            tx.register(listener);
            fail();
        } catch (PreparedTransactionException expected) {
        }

        verify(listener, never()).notify((Transaction) anyObject(), (TransactionLifecycleEvent) anyObject());
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        Transaction tx = new AbstractTransactionImpl();
        tx.commit();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        try {
            tx.register(listener);
            fail();
        } catch (DeadTransactionException expected) {

        }

        verify(listener, never()).notify((Transaction) anyObject(), (TransactionLifecycleEvent) anyObject());
    }

    @Test
    public void whenAlreadyAborted_thenDeadTransactionException() {
        Transaction tx = new AbstractTransactionImpl();
        tx.abort();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        try {
            tx.register(listener);
            fail();
        } catch (DeadTransactionException expected) {
        }

        verify(listener, never()).notify((Transaction) anyObject(), (TransactionLifecycleEvent) anyObject());
    }

    @Test
    public void scheduleFailsWithNullTask() {
        Transaction tx = new AbstractTransactionImpl();

        try {
            tx.register(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenCommit_thenPreAndPostCommitTasksAreCalled() {
        Transaction tx = new AbstractTransactionImpl();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);

        tx.commit();

        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.postCommit);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.preAbort);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.postAbort);
    }
}

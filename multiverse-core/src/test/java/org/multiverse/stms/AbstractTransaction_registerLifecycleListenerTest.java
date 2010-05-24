package org.multiverse.stms;

import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsActive;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_registerLifecycleListenerTest {

    @Test
    public void whenPrepared_thenPreparedTransactionException() {
        Transaction tx = new AbstractTransactionImpl();
        tx.prepare();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        tx.commit();

        verify(listener, never()).notify(tx, TransactionLifecycleEvent.PreCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PostCommit);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.PreAbort);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.PostAbort);
    }

    @Test
    public void whenAlreadyCommitted_thenDeadTransactionException() {
        Transaction tx = new AbstractTransactionImpl();
        tx.commit();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);

        try {
            tx.registerLifecycleListener(listener);
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
            tx.registerLifecycleListener(listener);
            fail();
        } catch (DeadTransactionException expected) {
        }

        verify(listener, never()).notify((Transaction) anyObject(), (TransactionLifecycleEvent) anyObject());
    }

    @Test
    public void scheduleFailsWithNullTask() {
        Transaction tx = new AbstractTransactionImpl();
        tx.start();

        try {
            tx.registerLifecycleListener(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenCommit_thenPreAndPostCommitTasksAreCalled() {
        Transaction tx = new AbstractTransactionImpl();
        tx.start();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        tx.commit();

        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PreCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PostCommit);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.PreAbort);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.PostAbort);
    }
}

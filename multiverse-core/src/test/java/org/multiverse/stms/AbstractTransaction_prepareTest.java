package org.multiverse.stms;

import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;

public class AbstractTransaction_prepareTest {

    @Test
    public void whenLifecycleListenerRegistered_thenPreCommitIsCalled() {
        AbstractTransaction tx = new AbstractTransactionImpl();
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        tx.prepare();

        assertIsPrepared(tx);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preCommit);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.postCommit);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.preAbort);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.preAbort);
    }

    @Test
    public void whenPreCommitTaskFails_thenAborted() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        RuntimeException expected = new RuntimeException();
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        doThrow(expected).when(listener).notify(tx, TransactionLifecycleEvent.preCommit);

        try {
            tx.prepare();
            fail();
        } catch (Exception found) {
        }

        assertIsAborted(tx);
        verify(tx, times(1)).doAbortPrepared();
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preCommit);
        verify(listener, never()).notify(tx, TransactionLifecycleEvent.postCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preAbort);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preAbort);
    }

    @Test
    public void whenExceptionThrown_thenAbort() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        RuntimeException expected = new RuntimeException();
        doThrow(expected).when(tx).doPrepare();

        try {
            tx.prepare();
            fail();
        } catch (Exception found) {
            assertSame(expected, found);
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAlreadyPrepared_callIgnored() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.prepare();
        reset(tx);

        tx.prepare();
        verify(tx, times(0)).doPrepare();
        assertIsPrepared(tx);
    }

    @Test
    public void whenAlreadyPrepared_noLifecycleListenersCalled() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);
        tx.prepare();

        reset(listener);
        tx.prepare();
        verify(listener, times(0)).notify((Transaction) any(), (TransactionLifecycleEvent) any());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.abort();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        verify(tx, times(0)).doPrepare();
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.commit();

        reset(tx);
        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        verify(tx, times(0)).doPrepare();
    }
}

package org.multiverse.stms;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.clock.StrictPrimitiveClock;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_commitTest {

    private StrictPrimitiveClock clock;

    @Before
    public void setUp() {
        clock = new StrictPrimitiveClock(1);
    }

    @Test
    public void whenPrepareThrowsException_thenAbort() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.start();

        RuntimeException expected = new RuntimeException();
        doThrow(expected).when(tx).doPrepare();

        try {
            tx.commit();
            fail();
        } catch (RuntimeException found) {
            assertSame(expected, found);
            assertIsAborted(tx);
        }
    }

    @Test
    public void whenActive_listenersAreNotified() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.start();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PreCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PostCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.PreAbort);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.PostAbort);
    }

    @Test
    public void whenPreCommitTaskFails_thenAbort() {
        Transaction tx = new AbstractTransactionImpl();
        tx.start();

        RuntimeException exception = new RuntimeException();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        doThrow(exception).when(listener).notify(tx, TransactionLifecycleEvent.PreCommit);

        tx.registerLifecycleListener(listener);

        try {
            tx.commit();
            fail();
        } catch (RuntimeException found) {
            assertSame(exception, found);
        }

        assertIsAborted(tx);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PreCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.PostCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PreAbort);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PostAbort);
    }

    @Test
    public void whenPostCommitTaskFails_exceptionThrown() {
        Transaction tx = new AbstractTransactionImpl();
        tx.start();

        RuntimeException exception = new RuntimeException();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        doThrow(exception).when(listener).notify(tx, TransactionLifecycleEvent.PostCommit);

        tx.registerLifecycleListener(listener);

        try {
            tx.commit();
            fail();
        } catch (RuntimeException found) {
            assertSame(exception, found);
        }

        assertIsCommitted(tx);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PreCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PostCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.PreAbort);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.PostAbort);
    }

    @Test
    public void whenPrepared() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.prepare();

        tx.commit();
        verify(tx, times(1)).makeChangesPermanent();
    }

    @Test
    public void whenDoCommitPreparedFails_thenAbort() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.prepare();

        RuntimeException expected = new RuntimeException();
        doThrow(expected).when(tx).makeChangesPermanent();

        try {
            tx.commit();
            fail();
        } catch (Exception found) {
            assertSame(expected, found);
        }

        verify(tx, times(1)).doAbortPrepared();
        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenIgnore() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.commit();

        long version = clock.getVersion();
        tx.commit();
        assertIsCommitted(tx);
        assertEquals(version, clock.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.abort();

        long version = clock.getVersion();
        try {
            tx.commit();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertEquals(version, clock.getVersion());
    }
}

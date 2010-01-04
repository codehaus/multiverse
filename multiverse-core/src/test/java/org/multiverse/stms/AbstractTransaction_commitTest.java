package org.multiverse.stms;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionLifecycleEvent;
import org.multiverse.api.TransactionLifecycleListener;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.clock.StrictClock;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_commitTest {

    private StrictClock clock;

    @Before
    public void setUp() {
        clock = new StrictClock(1);
    }

    @Test
    public void whenOnCommitThrowsException_thenAbort() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());

        RuntimeException expected = new RuntimeException();
        doThrow(expected).when(tx).doCommit();

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

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.register(listener);
        tx.commit();

        assertIsCommitted(tx);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.postCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.preAbort);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.postAbort);
    }

    @Test
    public void whenPreCommitTaskFails_thenAbort() {
        Transaction tx = new AbstractTransactionImpl();

        RuntimeException exception = new RuntimeException();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        doThrow(exception).when(listener).notify(tx, TransactionLifecycleEvent.preCommit);

        tx.register(listener);

        try {
            tx.commit();
            fail();
        } catch (RuntimeException found) {
            assertSame(exception, found);
        }

        assertIsAborted(tx);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.postCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preAbort);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.postAbort);
    }

    @Test
    public void whenPostCommitTaskFails_exceptionThrown(){
        Transaction tx = new AbstractTransactionImpl();

        RuntimeException exception = new RuntimeException();

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        doThrow(exception).when(listener).notify(tx, TransactionLifecycleEvent.postCommit);

        tx.register(listener);

        try {
            tx.commit();
            fail();
        } catch (RuntimeException found) {
            assertSame(exception, found);
        }

        assertIsCommitted(tx);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preCommit);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.postCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.preAbort);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.postAbort);
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

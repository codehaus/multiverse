package org.multiverse.stms;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionLifecycleEvent;
import org.multiverse.api.TransactionLifecycleListener;
import org.multiverse.utils.clock.StrictPrimitiveClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsActive;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_restartTest {

    private StrictPrimitiveClock clock;

    @Before
    public void setUp() {
        clock = new StrictPrimitiveClock(1);
    }

    @Test
    public void whenActive_thenTransactionRestarted() {
        Transaction tx = new AbstractTransactionImpl(clock);
        long version = clock.getVersion();

        tx.restart();

        assertIsActive(tx);
        assertEquals(version, clock.getVersion());
    }

    @Test
    public void whenActive_thenListenersAreNotified() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        reset(listener);

        tx.restart();

        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preAbort);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.postAbort);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.preCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.postCommit);
    }

    @Test
    public void whenActiveAndPreAbortListenerFails_thenTransactionAborted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        doThrow(new RuntimeException()).when(listener).notify(tx, TransactionLifecycleEvent.preAbort);

        try {
            tx.restart();
            fail();
        } catch (RuntimeException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenActiveAndPostAbortListenerFails_thenTransactionAborted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        doThrow(new RuntimeException()).when(listener).notify(tx, TransactionLifecycleEvent.postAbort);

        try {
            tx.restart();
            fail();
        } catch (RuntimeException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPreparedAndPreAbortListenerFails_thenTransactionAborted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        doThrow(new RuntimeException()).when(listener).notify(tx, TransactionLifecycleEvent.preAbort);

        tx.prepare();
        try {
            tx.restart();
            fail();
        } catch (RuntimeException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPreparedAndPostAbortListenerFails_thenTransactionAborted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        doThrow(new RuntimeException()).when(listener).notify(tx, TransactionLifecycleEvent.postAbort);

        tx.prepare();
        try {
            tx.restart();
            fail();
        } catch (RuntimeException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPrepared_thenTransactionIsRestarted() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.prepare();

        long version = clock.getVersion();

        tx.restart();
        assertIsActive(tx);
        assertEquals(version, clock.getVersion());
    }

    @Test
    public void whenCommitted_thenTransactionIsRestarted() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.commit();

        long version = clock.getVersion();

        tx.restart();

        assertIsActive(tx);
        assertEquals(version, clock.getVersion());
    }

    @Test
    public void whenCommitted_thenNoScheduledTasksAreExecuted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);
        tx.commit();

        reset(listener);

        tx.restart();
        verify(listener, never()).notify((Transaction) any(), (TransactionLifecycleEvent) any());
    }

    @Test
    public void whenAbortedTransactionIsRestarted() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.abort();

        long version = clock.getVersion();

        tx.restart();

        assertIsActive(tx);
        assertEquals(version, clock.getVersion());
    }

    @Test
    public void whenAborted_thenNoListenersExecuted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);
        tx.abort();

        reset(listener);

        tx.restart();
        verify(listener, never()).notify((Transaction) any(), (TransactionLifecycleEvent) any());
    }
}

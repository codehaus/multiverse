package org.multiverse.stms;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.clock.StrictPrimitiveClock;
import org.multiverse.api.lifecycle.TransactionLifecycleEvent;
import org.multiverse.api.lifecycle.TransactionLifecycleListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsNew;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_resetTest {

    private StrictPrimitiveClock clock;

    @Before
    public void setUp() {
        clock = new StrictPrimitiveClock(1);
    }

    @Test
    public void whenNew() {
        Transaction tx = new AbstractTransactionImpl(clock);
        clock.tick();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenActive() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.start();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenActive_thenListenersAreNotified() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        reset(listener);

        tx.reset();

        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PreAbort);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.PostAbort);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.PreCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.PostCommit);
    }

    @Test
    public void whenActiveAndPreAbortListenerFails_thenTransactionAborted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);

        doThrow(new RuntimeException()).when(listener).notify(tx, TransactionLifecycleEvent.PreAbort);

        try {
            tx.reset();
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

        doThrow(new RuntimeException()).when(listener).notify(tx, TransactionLifecycleEvent.PostAbort);

        try {
            tx.reset();
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

        doThrow(new RuntimeException()).when(listener).notify(tx, TransactionLifecycleEvent.PreAbort);

        tx.prepare();
        try {
            tx.reset();
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

        doThrow(new RuntimeException()).when(listener).notify(tx, TransactionLifecycleEvent.PostAbort);

        tx.prepare();
        try {
            tx.reset();
            fail();
        } catch (RuntimeException ex) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenPrepared_thenTransactionIsRestarted() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.prepare();

        tx.reset();
        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenCommitted_thenTransactionIsRestarted() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.commit();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenCommitted_thenNoScheduledTasksAreExecuted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);
        tx.commit();

        reset(listener);

        tx.reset();
        verify(listener, never()).notify((Transaction) any(), (TransactionLifecycleEvent) any());
    }

    @Test
    public void whenAbortedTransactionIsRestarted() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.abort();

        tx.reset();

        assertIsNew(tx);
        assertEquals(0, tx.getReadVersion());
    }

    @Test
    public void whenAborted_thenNoListenersExecuted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        TransactionLifecycleListener listener = mock(TransactionLifecycleListener.class);
        tx.registerLifecycleListener(listener);
        tx.abort();

        reset(listener);

        tx.reset();
        verify(listener, never()).notify((Transaction) any(), (TransactionLifecycleEvent) any());
    }
}

package org.multiverse.stms;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionLifecycleEvent;
import org.multiverse.api.TransactionLifecycleListener;
import org.multiverse.utils.clock.StrictClock;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsActive;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_restartTest {

    private StrictClock clock;

    @Before
    public void setUp() {
        clock = new StrictClock(1);
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
        tx.register(listener);

        reset(listener);

        tx.restart();

        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.preAbort);
        verify(listener, times(1)).notify(tx, TransactionLifecycleEvent.postAbort);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.preCommit);
        verify(listener, times(0)).notify(tx, TransactionLifecycleEvent.postCommit);
    }

    @Test
    @Ignore
    public void whenPrepared_then() {

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
        tx.register(listener);
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
        tx.register(listener);
        tx.abort();

        reset(listener);

        tx.restart();
        verify(listener, never()).notify((Transaction) any(), (TransactionLifecycleEvent) any());
    }
}

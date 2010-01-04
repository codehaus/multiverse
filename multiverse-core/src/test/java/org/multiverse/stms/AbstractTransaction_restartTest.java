package org.multiverse.stms;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.utils.clock.StrictClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.testIncomplete;

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
    public void callOnActiveTransaction() {
        Transaction tx = new AbstractTransactionImpl(clock);
        long version = clock.getVersion();

        tx.restart();

        assertIsActive(tx);
        assertEquals(version, clock.getVersion());
    }

    public void when_TasksAreReset(){
        
    }

    @Test
    public void whenActiveScheduledTasksExecuted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        testIncomplete();

        /*
        Runnable preAbortTask = mock(Runnable.class);
        Runnable postAbortTask = mock(Runnable.class);
        Runnable preCommitTask = mock(Runnable.class);
        Runnable postCommitTask = mock(Runnable.class);

        t.schedule(preAbortTask, TransactionLifecycleEvent.preAbort);
        t.schedule(postAbortTask, TransactionLifecycleEvent.postAbort);
        t.schedule(preCommitTask, TransactionLifecycleEvent.preCommit);
        t.schedule(postCommitTask, TransactionLifecycleEvent.postCommit);

        t.abortAndReturnRestarted();

        verify(preAbortTask).run();
        verify(postAbortTask).run();
        verify(preCommitTask, never()).run();
        verify(postCommitTask, never()).run();   */
    }

    @Test
    public void whenCommittedTransactionIsRestarted() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.commit();

        long version = clock.getVersion();

        tx.restart();

        assertIsActive(tx);
        assertEquals(version, clock.getVersion());
    }

    @Test
    public void whenCommittedNoScheduledTasksAreExecuted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        Runnable preAbortTask = mock(Runnable.class);
        Runnable postAbortTask = mock(Runnable.class);
        Runnable preCommitTask = mock(Runnable.class);
        Runnable postCommitTask = mock(Runnable.class);

        /*

        t.schedule(preAbortTask, TransactionLifecycleEvent.preAbort);
        t.schedule(postAbortTask, TransactionLifecycleEvent.postAbort);
        t.schedule(preCommitTask, TransactionLifecycleEvent.preCommit);
        t.schedule(postCommitTask, TransactionLifecycleEvent.postCommit);

        //lets do the abort and reset the mocks to clean unwanted mocking
        t.commit();
        reset(preCommitTask, postCommitTask);

        //now do the abortAndRestart
        t.abortAndReturnRestarted();

        //make sure that there have not been any calls on the tasks
        verify(preAbortTask, never()).run();
        verify(postAbortTask, never()).run();
        verify(preCommitTask, never()).run();
        verify(postCommitTask, never()).run();

        */
        testIncomplete();
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
    public void whenAbortedNoTasksAreExecuted() {
        Transaction tx = new AbstractTransactionImpl(clock);

        Runnable preAbortTask = mock(Runnable.class);
        Runnable postAbortTask = mock(Runnable.class);
        Runnable preCommitTask = mock(Runnable.class);
        Runnable postCommitTask = mock(Runnable.class);
        /*

        t.schedule(preAbortTask, TransactionLifecycleEvent.preAbort);
        t.schedule(postAbortTask, TransactionLifecycleEvent.postAbort);
        t.schedule(preCommitTask, TransactionLifecycleEvent.preCommit);
        t.schedule(postCommitTask, TransactionLifecycleEvent.postCommit);

        //lets do the abort and reset the mocks to clean unwanted mocking
        t.abort();
        reset(preAbortTask, postAbortTask);

        //now do the abortAndRestart
        t.abortAndReturnRestarted();

        //make sure that there have not been any calls on the tasks
        verify(preAbortTask, never()).run();
        verify(postAbortTask, never()).run();
        verify(preCommitTask, never()).run();
        verify(postCommitTask, never()).run();
        */

        testIncomplete();
    }
}

package org.multiverse.commitbarriers;

import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

public class CountdownCommitBarrier_registerOnCommitTaskTest {

    @Test
    public void whenNullTask_thenNullPointerException() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);

        try {
            barrier.registerOnCommitTask(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
    }

    @Test
    public void whenAborted_thenTaskNotExecuted() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);
        Runnable task = mock(Runnable.class);

        barrier.registerOnCommitTask(task);
        barrier.abort();

        verify(task, never()).run();
    }

    @Test
    public void whenCommitted_thenTaskExecuted() throws InterruptedException {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);
        Runnable task = mock(Runnable.class);

        barrier.registerOnCommitTask(task);
        barrier.awaitCommit();

        verify(task, times(1)).run();
    }

    @Test
    public void whenTaskThrowsRuntimeException_thenOtherTasksNotExecuted() throws InterruptedException {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);
        Runnable task1 = mock(Runnable.class);
        doThrow(new FakeException()).when(task1).run();
        Runnable task2 = mock(Runnable.class);

        barrier.registerOnCommitTask(task1);
        barrier.registerOnCommitTask(task2);

        try {
            barrier.awaitCommit();
            fail();
        } catch (FakeException expected) {
        }

        verify(task2, never()).run();
    }

    static class FakeException extends RuntimeException {
    }

    @Test
    public void whenCommitted_thenClosedCommitBarrierException() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(0);

        Runnable task = mock(Runnable.class);
        try {
            barrier.registerOnCommitTask(task);
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }

        assertTrue(barrier.isCommitted());
        verify(task, never()).run();
    }

    @Test
    public void whenAborted_thenClosedCommitBarrierException() {
        CountdownCommitBarrier barrier = new CountdownCommitBarrier(1);
        barrier.abort();

        Runnable task = mock(Runnable.class);
        try {
            barrier.registerOnCommitTask(task);
            fail();
        } catch (ClosedCommitBarrierException expected) {
        }

        assertTrue(barrier.isAborted());
        verify(task, never()).run();
    }
}

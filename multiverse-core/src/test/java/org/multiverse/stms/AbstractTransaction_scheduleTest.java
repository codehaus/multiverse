package org.multiverse.stms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.multiverse.TestUtils.*;
import org.multiverse.api.ScheduleType;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_scheduleTest {

    @Test
    public void scheduleFailsWithNullTask() {
        Transaction t = new AbstractTransactionImpl();

        try {
            t.schedule(null, ScheduleType.postAbort);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(t);
    }

    @Test
    public void scheduleFailsWithNullScheduleType() {
        Transaction t = new AbstractTransactionImpl();

        TestTask testTask = new TestTask();

        try {
            t.schedule(testTask, null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(t);
        assertEquals(0, testTask.executionCount);
    }

    @Test
    public void preCommitTaskIsExecutedBeforeCommit() {
        TestTask task = new TestTask();
        Transaction t = new AbstractTransactionImpl();
        t.schedule(task, ScheduleType.preCommit);
        t.commit();

        //todo: check that it is done before commit
        assertIsCommitted(t);
        assertEquals(1, task.executionCount);
    }

    @Test
    public void postCommitTaskIsExecutedAfterCommit() {
        TestTask task = new TestTask();
        Transaction t = new AbstractTransactionImpl();
        t.schedule(task, ScheduleType.postCommit);
        t.commit();

        //todo: check that it is done after commit
        assertIsCommitted(t);
        assertEquals(1, task.executionCount);
    }

    @Test
    public void preAbortTaskIsExecutedBeforeAbort() {
        TestTask task = new TestTask();
        Transaction t = new AbstractTransactionImpl();
        t.schedule(task, ScheduleType.preAbort);
        t.abort();

        //todo: check that it is done before abort
        assertIsAborted(t);
        assertEquals(1, task.executionCount);
    }

    @Test
    public void postAbortTaskIsExecutedAfterAbort() {
        TestTask task = new TestTask();
        Transaction t = new AbstractTransactionImpl();
        t.schedule(task, ScheduleType.postAbort);
        t.abort();

        //todo: check that it is done after abort
        assertIsAborted(t);
        assertEquals(1, task.executionCount);
    }

    @Test
    public void abortTasksAreNotExecutedOnSuccessfulCommit() {
        TestTask task1 = new TestTask();
        TestTask task2 = new TestTask();

        Transaction t = new AbstractTransactionImpl();
        t.schedule(task1, ScheduleType.preAbort);
        t.schedule(task2, ScheduleType.postAbort);

        t.commit();

        assertEquals(0, task1.executionCount);
        assertEquals(0, task2.executionCount);
    }

    @Test
    public void commitTasksAreNotExecutedOnAbort() {
        TestTask task1 = new TestTask();
        TestTask task2 = new TestTask();

        Transaction t = new AbstractTransactionImpl();
        t.schedule(task1, ScheduleType.postCommit);
        t.schedule(task2, ScheduleType.preCommit);

        t.abort();

        assertEquals(0, task1.executionCount);
        assertEquals(0, task2.executionCount);
    }

    /*
    @Test
    public void failingPreCommitTaskDoesAbortCommit() {
        Transaction t = new AbstractTransactionImpl();

        TestTask preAbortTask = new TestTask();
        TestTask postAbortTask = new TestTask();

        t.schedule(preAbortTask, ScheduleType.preAbort);
        t.schedule(postAbortTask, ScheduleType.postAbort);
        t.schedule(new FailingTask(), ScheduleType.preCommit);

        try {
            t.commit();
            fail();
        } catch (FailException expected) {
        }

        assertIsAborted(t);
        assertEquals(1, preAbortTask.executionCount);
        assertEquals(1, postAbortTask.executionCount);
    } */

    @Test
    public void failingPostCommitTaskDoesNotRollbackCommit() {
        Transaction t = new AbstractTransactionImpl();
        t.schedule(new FailingTask(), ScheduleType.postCommit);
        try {
            t.commit();
            fail();
        } catch (FailException expected) {
        }

        assertIsCommitted(t);
    }

    @Test
    public void scheduleTaskFailsOnCommittedTransaction() {
        scheduleTaskFailsOnCommittedTransaction(ScheduleType.postAbort);
        scheduleTaskFailsOnCommittedTransaction(ScheduleType.preAbort);
        scheduleTaskFailsOnCommittedTransaction(ScheduleType.postCommit);
        scheduleTaskFailsOnCommittedTransaction(ScheduleType.preCommit);
    }

    public void scheduleTaskFailsOnCommittedTransaction(ScheduleType scheduleType) {
        Transaction t = new AbstractTransactionImpl();
        t.commit();

        TestTask task = new TestTask();
        try {
            t.schedule(task, scheduleType);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(t);
        assertEquals(0, task.executionCount);
    }

    @Test
    public void scheduleTaskFailsOnAbortedTransaction() {
        scheduleTaskFailsOnAbortedTransaction(ScheduleType.postAbort);
        scheduleTaskFailsOnAbortedTransaction(ScheduleType.preAbort);
        scheduleTaskFailsOnAbortedTransaction(ScheduleType.postCommit);
        scheduleTaskFailsOnAbortedTransaction(ScheduleType.preCommit);
    }

    public void scheduleTaskFailsOnAbortedTransaction(ScheduleType scheduleType) {
        Transaction t = new AbstractTransactionImpl();
        t.abort();

        TestTask task = new TestTask();
        try {
            t.schedule(task, scheduleType);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
        assertEquals(0, task.executionCount);
    }

    static class TestTask implements Runnable {
        int executionCount;

        @Override
        public void run() {
            executionCount++;
        }
    }

    static class FailingTask implements Runnable {
        @Override
        public void run() {
            throw new FailException();
        }
    }

    static class FailException extends RuntimeException {
    }
}

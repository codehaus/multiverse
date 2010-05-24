package org.multiverse.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.api.exceptions.Retry;
import org.multiverse.stms.AbstractTransactionImpl;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class StmUtilsTest {

    @Before
    public void setup() {
        clearThreadLocalTransaction();
    }

    @After
    public void teardown() {
        clearThreadLocalTransaction();
    }

    @Test(expected = Retry.class)
    public void retryWithTransactionThrowsRetryError() {
        Transaction t = mock(Transaction.class);
        setThreadLocalTransaction(t);

        retry();
    }

    @Test(expected = NoTransactionFoundException.class)
    public void retryWithoutTransactionFails() {
        retry();
    }


    @Test(expected = NoTransactionFoundException.class)
    public void abort_whenNoTransactionActive_thenNoTransactionFoundException() {
        abort();
        fail();
    }

    @Test
    public void abort_whenTransactionActive_thenTransactionAborted() {
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);

        abort();
        verify(tx, times(1)).abort();
    }

    @Test(expected = NoTransactionFoundException.class)
    public void prepare_whenNoTransactionActive_thenNoTransactionFoundException() {
        prepare();
        fail();
    }

    @Test
    public void prepare_whenTransactionActive_thenTransactionPrepared() {
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);
        prepare();
        verify(tx, times(1)).prepare();
    }

    @Test(expected = NoTransactionFoundException.class)
    public void commit_whenNoTransactionActive_thenNoTransactionFoundException() {
        commit();
        fail();
    }

    @Test
    public void commit_whenTransactionActive_thenTransactionCommitted() {
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);
        commit();
        verify(tx, times(1)).commit();
    }

    @Test(expected = NoTransactionFoundException.class)
    public void scheduleDeferredTask_whenNoTransactionAvailable_thenNoTransactionFoundException() {
        Runnable task = mock(Runnable.class);
        scheduleDeferredTask(task);
        fail();
    }

    @Test(expected = NullPointerException.class)
    public void scheduleDeferredTask_whenTaskNull_thenNullPointerException() {
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);

        scheduleDeferredTask(null);
    }

    @Test
    public void scheduleDeferredTask_whenTransactionAvailable() {
        Runnable task = mock(Runnable.class);

        Transaction tx = new AbstractTransactionImpl();
        tx.start();
        setThreadLocalTransaction(tx);

        scheduleDeferredTask(task);

        verify(task, times(0)).run();

        tx.commit();

        verify(task, times(1)).run();
    }

    @Test(expected = NoTransactionFoundException.class)
    public void scheduleCompensatingTask_whenNoTransactionAvailable_thenNoTransactionFoundException() {
        Runnable task = mock(Runnable.class);
        scheduleCompensatingTask(task);
    }

    @Test(expected = NullPointerException.class)
    public void scheduleCompensatingTask_whenTaskNull_thenNullPointerException() {
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);

        scheduleCompensatingTask(null);
    }

    @Test
    public void scheduleCompensatingTask_whenTransactionAvailable() {
        Runnable task = mock(Runnable.class);

        Transaction tx = new AbstractTransactionImpl();
        setThreadLocalTransaction(tx);

        scheduleCompensatingTask(task);

        verify(task, times(0)).run();

        tx.abort();

        verify(task, times(1)).run();
    }

}


package org.multiverse.api;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.multiverse.api.StmUtils.*;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.api.exceptions.RetryError;
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

    @Test(expected = RetryError.class)
    public void retryWithTransactionThrowsRetryError() {
        Transaction t = mock(Transaction.class);
        setThreadLocalTransaction(t);

        retry();
    }

    @Test(expected = NoTransactionFoundException.class)
    public void retryWithoutTransactionFails() {
        retry();
    }

    @Test
    public void deferredExecuteIsForwardedToTransactionThreadLocal() {
        Runnable task = mock(Runnable.class);

        Transaction t = mock(Transaction.class);
        setThreadLocalTransaction(t);
        deferredExecute(task);

        verify(t).schedule(task, ScheduleType.postCommit);
    }

    @Test(expected = NoTransactionFoundException.class)
    public void deferredExecuteWithoutTransactionFails() {
        Runnable task = mock(Runnable.class);
        deferredExecute(task);
    }

    @Test(expected = NoTransactionFoundException.class)
    public void compensatingExecuteWithoutTransactionFails() {
        Runnable task = mock(Runnable.class);
        compensatingExecute(task);
    }

    @Test
    public void compensatingExecuteIsForwardedToTransactionThreadLocal() {
        Runnable task = mock(Runnable.class);

        Transaction t = mock(Transaction.class);
        setThreadLocalTransaction(t);
        compensatingExecute(task);

        verify(t).schedule(task, ScheduleType.postAbort);
    }
}

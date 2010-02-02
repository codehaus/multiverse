package org.multiverse.api;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class ThreadLocalTransaction_setThreadLocalTransactionTest {

    @Test
    public void whenTransactionAvailable_thenNotAborted() {
        Transaction tx1 = mock(Transaction.class);
        setThreadLocalTransaction(tx1);

        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);

        verify(tx1, never()).abort();
    }

    @Test
    public void whenNewTransactionNull_thenCleared() {
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);

        setThreadLocalTransaction(null);
        assertNull(getThreadLocalTransaction());
    }

    @Test
    public void whenNewTransactionNotNull_thenValueUpdated() {

    }
}

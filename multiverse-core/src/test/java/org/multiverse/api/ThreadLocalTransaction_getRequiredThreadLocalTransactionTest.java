package org.multiverse.api;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.NoTransactionFoundException;

import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;
import static org.multiverse.api.ThreadLocalTransaction.getRequiredThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class ThreadLocalTransaction_getRequiredThreadLocalTransactionTest {

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
    }

    @Test(expected = NoTransactionFoundException.class)
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        getRequiredThreadLocalTransaction();
    }

    @Test
    public void whenTransactionAvailable_thenItIsReturned() {
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);

        Transaction found = getRequiredThreadLocalTransaction();
        assertSame(tx, found);
        //we need to make sure that the transaction is returned without looking at the transaction state.
        verify(tx, never()).getStatus();
    }
}

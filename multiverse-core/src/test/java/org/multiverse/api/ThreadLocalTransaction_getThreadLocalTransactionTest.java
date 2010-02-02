package org.multiverse.api;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class ThreadLocalTransaction_getThreadLocalTransactionTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNotAvailable_thenNullReturned() {
        Transaction found = getThreadLocalTransaction();
        assertNull(found);
    }

    @Test
    public void whenAvailable_thenReturned(){
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);

        Transaction found = getThreadLocalTransaction();
        assertSame(tx, found);
        verify(tx, never()).getStatus();
    }
}

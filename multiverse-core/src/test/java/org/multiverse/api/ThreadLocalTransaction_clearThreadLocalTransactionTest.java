package org.multiverse.api;

import org.junit.Test;
import org.multiverse.TestThread;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.multiverse.TestUtils.joinAll;
import static org.multiverse.TestUtils.startAll;
import static org.multiverse.api.ThreadLocalTransaction.*;

/**
 * @author Peter Veentjer
 */
public class ThreadLocalTransaction_clearThreadLocalTransactionTest {

    @Test
    public void whenTransactionAvailable_thenItIsNotAborted(){
        Transaction tx = mock(Transaction.class);
        setThreadLocalTransaction(tx);

        clearThreadLocalTransaction();
        verify(tx, never()).abort();
    }

    @Test
    public void whenTransactionAvailable_thenItIsCleared() {
        Transaction t = mock(Transaction.class);
        setThreadLocalTransaction(t);

        clearThreadLocalTransaction();
        assertNull(getThreadLocalTransaction());
    }
}

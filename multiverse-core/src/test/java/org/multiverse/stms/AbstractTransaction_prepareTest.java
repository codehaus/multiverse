package org.multiverse.stms;

import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;

public class AbstractTransaction_prepareTest {

    @Test
    public void whenExceptionThrown_thenAbort() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        RuntimeException expected = new RuntimeException();
        doThrow(expected).when(tx).doPrepare();

        try {
            tx.prepare();
            fail();
        } catch (Exception found) {
            assertSame(expected, found);
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.abort();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        verify(tx, times(0)).doPrepare();
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.commit();

        try {
            tx.prepare();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
        verify(tx, times(0)).doPrepare();
    }
}

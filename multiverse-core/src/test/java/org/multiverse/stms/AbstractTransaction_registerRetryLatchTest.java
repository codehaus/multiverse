package org.multiverse.stms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.multiverse.TestUtils.*;

import org.multiverse.api.Latch;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.latches.CheapLatch;

public class AbstractTransaction_registerRetryLatchTest {

    @Test
    public void whenActive_thenNullPointerException(){
        AbstractTransactionImpl tx = new AbstractTransactionImpl();

        try {
            tx.registerRetryLatch(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        AbstractTransactionImpl tx = new AbstractTransactionImpl();
        tx.commit();

        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(tx);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        AbstractTransactionImpl tx = new AbstractTransactionImpl();
        tx.abort();

        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertFalse(latch.isOpen());
    }
}

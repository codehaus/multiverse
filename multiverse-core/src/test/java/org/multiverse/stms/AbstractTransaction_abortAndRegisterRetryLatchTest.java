package org.multiverse.stms;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.multiverse.TestUtils.*;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

public class AbstractTransaction_abortAndRegisterRetryLatchTest {

    @Test
    public void nullLatchFails() {
        AbstractTransactionImpl transaction = new AbstractTransactionImpl();


        try {
            transaction.abortAndRegisterRetryLatch(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(transaction);
    }

    @Test
    public void failsIfCommitted() {
        AbstractTransactionImpl transaction = new AbstractTransactionImpl();
        transaction.commit();

        Latch latch = new CheapLatch();

        try {
            transaction.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(transaction);
        assertFalse(latch.isOpen());
    }

    @Test
    public void failsIfAborted() {
        AbstractTransactionImpl transaction = new AbstractTransactionImpl();
        transaction.abort();

        Latch latch = new CheapLatch();

        try {
            transaction.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(transaction);
        assertFalse(latch.isOpen());
    }
}

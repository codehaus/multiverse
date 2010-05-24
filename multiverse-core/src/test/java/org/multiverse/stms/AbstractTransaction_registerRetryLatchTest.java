package org.multiverse.stms;

import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.*;

public class AbstractTransaction_registerRetryLatchTest {

    @Test
    public void whenActiveAndNullLatch_thenNullPointerException() {
        AbstractTransactionImpl tx = new AbstractTransactionImpl();

        try {
            tx.registerRetryLatch(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertIsActive(tx);
    }

    @Test
    public void whenActiveAndDoRegisterRetryLatchReturnsTrue_thenSuccess() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.start();

        Latch latch = new CheapLatch();

        when(tx.doRegisterRetryLatch(latch, tx.getReadVersion() + 1)).thenReturn(true);

        tx.registerRetryLatch(latch);

        assertIsActive(tx);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenActiveAndDoRegisterRetryLatchReturnsFalse_thenNoRetryPossibleException() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());

        Latch latch = new CheapLatch();

        when(tx.doRegisterRetryLatch(latch, tx.getReadVersion() + 1)).thenReturn(false);

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsActive(tx);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenPreparedAndDoRegisterRetryLatchReturnsTrue_thenSuccess() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.prepare();

        Latch latch = new CheapLatch();

        when(tx.doRegisterRetryLatch(latch, tx.getReadVersion() + 1)).thenReturn(true);

        tx.registerRetryLatch(latch);

        assertIsPrepared(tx);
        assertFalse(latch.isOpen());
    }

    @Test
    public void whenPreparedAndDoRegisterRetryLatchReturnsFalse_thenNoRetryPossibleException() {
        AbstractTransaction tx = spy(new AbstractTransactionImpl());
        tx.prepare();

        Latch latch = new CheapLatch();

        when(tx.doRegisterRetryLatch(latch, tx.getReadVersion() + 1)).thenReturn(false);

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException expected) {
        }

        assertIsPrepared(tx);
        assertFalse(latch.isOpen());
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

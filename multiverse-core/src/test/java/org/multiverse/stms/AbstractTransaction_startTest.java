package org.multiverse.stms;

import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_startTest {

    @Test
    public void whenNew_thenStart() {
        Transaction tx = new AbstractTransactionImpl();

        tx.start();
        assertIsActive(tx);
    }

    @Test
    public void whenActive_thenIgnore() {
        Transaction tx = new AbstractTransactionImpl();
        tx.start();

        tx.start();
        assertIsActive(tx);
    }

    @Test
    public void whenPrepared() {
        Transaction tx = new AbstractTransactionImpl();
        tx.prepare();

        try {
            tx.start();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsPrepared(tx);
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        Transaction tx = new AbstractTransactionImpl();
        tx.abort();

        try {
            tx.start();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        Transaction tx = new AbstractTransactionImpl();
        tx.commit();

        try {
            tx.start();
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
    }
}

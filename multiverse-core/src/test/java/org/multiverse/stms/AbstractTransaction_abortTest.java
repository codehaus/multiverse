package org.multiverse.stms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.clock.StrictClock;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_abortTest {

    private StrictClock clock;

    @Before
    public void setUp() {
        clock = new StrictClock(1);
    }

    @Test
    public void abortStartedTransaction() {
        Transaction t = new AbstractTransactionImpl();
        long startVersion = clock.getTime();
        t.abort();
        assertEquals(startVersion, clock.getTime());
        assertIsAborted(t);
    }

    @Test
    public void abortCommittedTransactionShouldFail() {
        Transaction t = new AbstractTransactionImpl();
        t.commit();

        long startVersion = clock.getTime();
        try {
            t.abort();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertEquals(startVersion, clock.getTime());
        assertIsCommitted(t);
    }

    @Test
    public void abortAbortedTransactionIsIgnored() {
        Transaction t = new AbstractTransactionImpl();
        t.abort();

        long startVersion = clock.getTime();
        t.abort();
        assertEquals(startVersion, clock.getTime());
        assertIsAborted(t);
    }
}

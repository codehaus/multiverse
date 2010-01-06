package org.multiverse.stms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.*;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.clock.StrictClock;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_endOrAndStartElseTest {

    private StrictClock clock;

    @Before
    public void setUp() {
        clock = new StrictClock(1);
    }

    @Test
    public void abortStartedTransaction() {
        testIncomplete();
    }

    @Test
    public void test() {
        testIncomplete();
    }

    @Test
    public void startOrOnCommittedTransactionFails() {
        Transaction t = new AbstractTransactionImpl(clock);
        t.commit();

        long version = clock.getTime();
        try {
            t.endOrAndStartElse();
            fail();
        } catch (DeadTransactionException ex) {
        }
        assertIsCommitted(t);
        assertEquals(version, clock.getTime());
    }

    @Test
    public void startOrOnAbortedTransactionFails() {
        Transaction t = new AbstractTransactionImpl(clock);
        t.abort();

        long version = clock.getTime();
        try {
            t.endOrAndStartElse();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
        assertEquals(version, clock.getTime());
    }

}

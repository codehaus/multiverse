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
public class AbstractTransaction_startOrTest {

    private StrictClock clock;

    @Before
    public void setUp() {
        clock = new StrictClock(1);
    }

    @Test
    public void whenActive() {
        testIncomplete();
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        Transaction tx = new AbstractTransactionImpl(clock);
        tx.commit();

        long version = clock.getVersion();
        try {
            tx.startOr();
            fail();
        } catch (DeadTransactionException ex) {
        }
        assertIsCommitted(tx);
        assertEquals(version, clock.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        Transaction t = new AbstractTransactionImpl(clock);
        t.abort();

        long version = clock.getVersion();
        try {
            t.startOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
        assertEquals(version, clock.getVersion());
    }
}

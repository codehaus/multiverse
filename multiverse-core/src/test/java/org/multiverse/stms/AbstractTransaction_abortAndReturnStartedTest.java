package org.multiverse.stms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsActive;
import org.multiverse.api.Transaction;
import org.multiverse.utils.clock.StrictClock;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_abortAndReturnStartedTest {

    private StrictClock clock;

    @Before
    public void setUp() {
        clock = new StrictClock(1);
    }

    @Test
    public void startedTransaction() {
        Transaction t = new AbstractTransactionImpl(clock);
        long version = clock.getTime();

        Transaction result = t.abortAndReturnRestarted();

        assertSame(t, result);
        assertIsActive(t);
        assertEquals(version, clock.getTime());
    }

    @Test
    public void committedTransaction() {
        Transaction t = new AbstractTransactionImpl(clock);
        t.commit();

        long version = clock.getTime();

        Transaction result = t.abortAndReturnRestarted();

        assertSame(t, result);
        assertIsActive(t);
        assertEquals(version, clock.getTime());
    }

    @Test
    public void abortedTransaction() {
        Transaction t = new AbstractTransactionImpl(clock);
        t.abort();

        long version = clock.getTime();

        Transaction result = t.abortAndReturnRestarted();

        assertSame(t, result);
        assertIsActive(t);
        assertEquals(version, clock.getTime());
    }
}

package org.multiverse.stms;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.clock.StrictPrimitiveClock;
import org.multiverse.api.exceptions.DeadTransactionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_startOrTest {

    private StrictPrimitiveClock clock;

    @Before
    public void setUp() {
        clock = new StrictPrimitiveClock(1);
    }

    @Test
    public void whenActive() {
        testIncomplete();
    }

    @Test
    public void whenCommitted_thenDeadTransactionException() {
        AbstractTransaction tx = new AbstractTransactionImpl(clock);
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
        AbstractTransaction t = new AbstractTransactionImpl(clock);
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

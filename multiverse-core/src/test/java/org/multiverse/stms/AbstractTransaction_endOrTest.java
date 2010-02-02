package org.multiverse.stms;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.clock.StrictPrimitiveClock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.*;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_endOrTest {

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
            tx.endOr();
            fail();
        } catch (DeadTransactionException ex) {
        }
        assertIsCommitted(tx);
        assertEquals(version, clock.getVersion());
    }

    @Test
    public void whenAborted_thenDeadTransactionException() {
        AbstractTransaction tx = new AbstractTransactionImpl(clock);
        tx.abort();

        long version = clock.getVersion();
        try {
            tx.endOr();
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(tx);
        assertEquals(version, clock.getVersion());
    }
}

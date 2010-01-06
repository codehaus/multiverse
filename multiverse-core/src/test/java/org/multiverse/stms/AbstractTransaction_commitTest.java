package org.multiverse.stms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.CommitFailureException;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.utils.clock.StrictClock;

/**
 * @author Peter Veentjer
 */
public class AbstractTransaction_commitTest {

    private StrictClock clock;

    @Before
    public void setUp() {
        clock = new StrictClock(1);
    }

    @Ignore
    @Test
    public void commitOnStartedTransactionIsDelegated() {
        AbstractTransaction t = mock(AbstractTransaction.class);
        when(t.onCommit()).thenReturn(10L);

        long version = t.commit();
        assertEquals(10, version);
        assertIsCommitted(t);
    }

    @Ignore
    @Test
    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void commitOnStartedLeadsToAbortWhenExceptionIsThrow() {
        AbstractTransaction t = mock(AbstractTransaction.class);
        when(t.onCommit()).thenThrow(new CommitFailureException());

        try {
            t.commit();
            fail();
        } catch (CommitFailureException ex) {

        }
        assertIsAborted(t);
    }

    @Test
    public void commitOnCommittedTransactionIsIgnored() {
        Transaction t = new AbstractTransactionImpl(clock);
        t.commit();

        long version = clock.getTime();
        t.commit();
        assertIsCommitted(t);
        assertEquals(version, clock.getTime());
    }

    @Test
    public void commitOnAbortedTransactionFails() {
        Transaction t = new AbstractTransactionImpl(clock);
        t.abort();

        long version = clock.getTime();
        try {
            t.commit();
            fail();
        } catch (DeadTransactionException ex) {

        }

        assertIsAborted(t);
        assertEquals(version, clock.getTime());
    }
}

package org.multiverse.integrationtests.notification;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.integrationtests.Ref;

import static org.junit.Assert.fail;
import static org.multiverse.api.StmUtils.retry;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Tests that makes sure that when readtracking is disabled, that the retry functionality
 * is not working.
 *
 * @author Peter Veentjer
 */
public class WhenReadTrackingDisabledRetryTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NoRetryPossibleException.class)
    public void whenUsedUpdateTransaction() {
        Ref ref1 = new Ref();
        updateBlockingMethod(ref1);
        fail();
    }

    @Test(expected = NoRetryPossibleException.class)
    public void whenUnusedUpdateTransaction() {
        updateBlockingMethod();
        fail();

    }

    @TransactionalMethod(trackReads = false, readonly = false)
    public void updateBlockingMethod(Ref... refs) {
        for (Ref ref : refs) {
            ref.get();
        }

        retry();
    }

    @Test(expected = NoRetryPossibleException.class)
    public void whenUnusedReadonlyTransaction() {
        readonlyBlockingMethod();
        fail();
    }

    @Test(expected = NoRetryPossibleException.class)
    public void whenUsedReadonlyTransaction() {
        Ref ref1 = new Ref();
        readonlyBlockingMethod(ref1);
        fail();
    }

    @TransactionalMethod(trackReads = false, readonly = true)
    public void readonlyBlockingMethod(Ref... refs) {
        for (Ref ref : refs) {
            ref.get();
        }

        retry();
    }
}

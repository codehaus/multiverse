package org.multiverse.integrationtests.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.RetryTimeoutException;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TimeoutRollbackTest {

    private TransactionalInteger modifyRef;
    private TransactionalInteger awaitRef;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();

        modifyRef = new TransactionalInteger();
        awaitRef = new TransactionalInteger();
    }

    @Test
    public void test() {
        try {
            setAndTimeout();
            fail();
        } catch (RetryTimeoutException expected) {
        }

        assertEquals(0, modifyRef.get());
    }

    @TransactionalMethod(timeout = 1)
    public void setAndTimeout() {
        modifyRef.set(1);
        awaitRef.await(1000);
    }
}

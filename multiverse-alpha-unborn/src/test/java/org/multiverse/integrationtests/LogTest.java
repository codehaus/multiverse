package org.multiverse.integrationtests;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.TraceLevel;
import org.multiverse.api.TransactionConfiguration;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class LogTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        loggingMethod();
    }

    @TransactionalMethod(traceLevel = TraceLevel.course)
    public void loggingMethod() {
        TransactionConfiguration config = getThreadLocalTransaction().getConfiguration();
        assertEquals(TraceLevel.course, config.getTraceLevel());
    }
}

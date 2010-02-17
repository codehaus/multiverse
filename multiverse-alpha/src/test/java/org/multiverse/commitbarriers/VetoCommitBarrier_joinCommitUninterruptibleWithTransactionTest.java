package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.multiverse.TestUtils.clearCurrentThreadInterruptedStatus;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class VetoCommitBarrier_joinCommitUninterruptibleWithTransactionTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void whenAborted_thenIllegalStateException() {

    }


}

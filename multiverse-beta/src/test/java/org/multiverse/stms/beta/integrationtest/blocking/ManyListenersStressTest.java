package org.multiverse.stms.beta.integrationtest.blocking;

import org.junit.Before;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class ManyListenersStressTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }
}

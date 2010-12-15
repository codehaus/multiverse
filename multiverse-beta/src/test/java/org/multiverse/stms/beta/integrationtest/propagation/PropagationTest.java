package org.multiverse.stms.beta.integrationtest.propagation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class PropagationTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void test() {

    }
}

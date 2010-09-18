package org.multiverse.stms.beta.integrationtest.pessimistic;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class EnsureStressTest {
    private BetaStm stm;

    public void setUp(){
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void test(){}
}

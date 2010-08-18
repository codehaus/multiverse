package org.multiverse.stms.beta.integrationtest.traditionalsynchronization;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

public class ReentrantMutexStressTest {
    private BetaStm stm;

    @Before
    public void setUp(){
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void test(){}
}

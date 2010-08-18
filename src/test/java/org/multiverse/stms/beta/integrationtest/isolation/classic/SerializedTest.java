package org.multiverse.stms.beta.integrationtest.isolation.classic;

import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

public class SerializedTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Test
    public void setUp(){
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }
    
    @Test
    @Ignore
    public void test(){}
}

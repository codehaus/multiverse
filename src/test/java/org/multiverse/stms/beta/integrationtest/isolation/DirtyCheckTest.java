package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

public class DirtyCheckTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp(){
           stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void test(){

    }
}

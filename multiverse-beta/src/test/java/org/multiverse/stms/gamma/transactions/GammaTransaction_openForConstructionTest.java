package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.multiverse.stms.gamma.GammaStm;

public class GammaTransaction_openForConstructionTest<T extends GammaTransaction>{
    protected GammaStm stm;

    @Before
    public void setUp(){
        stm = new GammaStm();
    }
}

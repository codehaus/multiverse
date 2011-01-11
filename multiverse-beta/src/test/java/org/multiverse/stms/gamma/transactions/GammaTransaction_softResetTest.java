package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.multiverse.stms.gamma.GammaStm;

public abstract class GammaTransaction_softResetTest<T extends GammaTransaction> {

    protected GammaStm stm;

    @Before
    public void setUp(){
           stm = new GammaStm();
    }

    public abstract T newTransaction();
}

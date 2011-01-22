package org.multiverse.stms.gamma.transactions.lean;

import org.junit.Before;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

public abstract class LeanGammaTransaction_setAbortOnlyTest<T extends GammaTransaction> {

    public GammaStm stm;

    public abstract T newTransaction();

    @Before
    public void setUp() {
        stm = new GammaStm();
    }


}

package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.multiverse.stms.gamma.GammaStm;

public abstract class GammaTransaction_hardResetTest<T extends GammaTransaction> {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();
}

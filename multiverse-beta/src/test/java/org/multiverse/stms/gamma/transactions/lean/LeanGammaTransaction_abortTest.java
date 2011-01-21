package org.multiverse.stms.gamma.transactions.lean;

import org.junit.Before;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

public abstract class LeanGammaTransaction_abortTest<T extends GammaTransaction> {

    public GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    public abstract T newTransaction();
}

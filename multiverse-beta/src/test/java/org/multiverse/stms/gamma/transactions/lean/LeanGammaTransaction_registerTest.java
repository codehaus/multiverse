package org.multiverse.stms.gamma.transactions.lean;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

public abstract class LeanGammaTransaction_registerTest<T extends GammaTransaction> {

    public GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    public abstract T newTransaction();

    @Test
    public void whenNullListener_thenNullPointerException() {

    }

    @Test
    public void whenSuccess_thenSpeculativeConfigurationError() {

    }

    @Test
    public void whenCommitted() {

    }

    @Test
    public void whenAborted() {

    }
}

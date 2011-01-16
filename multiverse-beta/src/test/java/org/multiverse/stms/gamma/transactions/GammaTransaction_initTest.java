package org.multiverse.stms.gamma.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.gamma.GammaStm;

import static org.junit.Assert.assertSame;

public abstract class GammaTransaction_initTest<T extends GammaTransaction> {

    protected GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    protected abstract T newTransaction();

    @Test(expected = NullPointerException.class)
    public void whenNullConfig_thenNullPointerException() {
        T tx = newTransaction();

        tx.init(null);
    }

    @Test
    public void whenSuccess() {
        T tx = newTransaction();

        GammaTransactionConfiguration config = new GammaTransactionConfiguration(stm);

        tx.init(config);

        assertSame(config, tx.getConfiguration());
    }
}

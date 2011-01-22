package org.multiverse.stms.gamma.transactions.lean;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.SpeculativeConfigurationError;
import org.multiverse.api.lifecycle.TransactionListener;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertIsAborted;

public abstract class LeanGammaTransaction_setAbortOnlyTest<T extends GammaTransaction> {

    public GammaStm stm;

    public abstract T newTransaction();

    @Before
    public void setUp() {
        stm = new GammaStm();
    }

    @Test
    public void whenSetAbortOnlyCalled_thenSpeculativeConfigurationError() {
        T tx = newTransaction();
        TransactionListener listener = mock(TransactionListener.class);

        try {
            tx.register(listener);
            fail();
        } catch (SpeculativeConfigurationError expected) {
        }

        assertIsAborted(tx);
    }
}

package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;

public class BetaStmTransactionFactoryBuilder_argumentsTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullBackoffPolicy_thenNullPointerException() {
        stm.getTransactionFactoryBuilder().setBackoffPolicy(null);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullFamilyName_thenNullPointerException() {
        stm.getTransactionFactoryBuilder().setFamilyName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenMaxRetriesNegative_thenIllegalArgumentException() {
        stm.getTransactionFactoryBuilder().setMaxRetries(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullPessimisticLockLevel_thenNullPointerException() {
        stm.getTransactionFactoryBuilder().setPessimisticLockLevel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeSpinCount_thenIllegalArgumentException() {
        stm.getTransactionFactoryBuilder().setSpinCount(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTraceLevel_thenIllegalArgumentException() {
        stm.getTransactionFactoryBuilder().setTraceLevel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeTimeout_thenIllegalArgumentException() {
        stm.getTransactionFactoryBuilder().setTimeoutNs(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenAddNullPermanentListener_thenNullPointerException() {
        stm.getTransactionFactoryBuilder().addPermanentListener(null);
    }
}

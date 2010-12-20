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
        stm.createTransactionFactoryBuilder().setBackoffPolicy(null);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullFamilyName_thenNullPointerException() {
        stm.createTransactionFactoryBuilder().setFamilyName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenMaxRetriesNegative_thenIllegalArgumentException() {
        stm.createTransactionFactoryBuilder().setMaxRetries(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullLockLevel_thenNullPointerException() {
        stm.createTransactionFactoryBuilder().setLockLevel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeSpinCount_thenIllegalArgumentException() {
        stm.createTransactionFactoryBuilder().setSpinCount(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTraceLevel_thenIllegalArgumentException() {
        stm.createTransactionFactoryBuilder().setTraceLevel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeTimeout_thenIllegalArgumentException() {
        stm.createTransactionFactoryBuilder().setTimeoutNs(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenAddNullPermanentListener_thenNullPointerException() {
        stm.createTransactionFactoryBuilder().addPermanentListener(null);
    }
}

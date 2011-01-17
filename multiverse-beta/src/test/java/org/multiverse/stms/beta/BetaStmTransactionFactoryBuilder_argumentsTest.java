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
        stm.newTransactionFactoryBuilder().setBackoffPolicy(null);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullFamilyName_thenNullPointerException() {
        stm.newTransactionFactoryBuilder().setFamilyName(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenMaxRetriesNegative_thenIllegalArgumentException() {
        stm.newTransactionFactoryBuilder().setMaxRetries(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullLockLevel_thenNullPointerException() {
        stm.newTransactionFactoryBuilder().setLockLevel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeSpinCount_thenIllegalArgumentException() {
        stm.newTransactionFactoryBuilder().setSpinCount(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTraceLevel_thenIllegalArgumentException() {
        stm.newTransactionFactoryBuilder().setTraceLevel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenNegativeTimeout_thenIllegalArgumentException() {
        stm.newTransactionFactoryBuilder().setTimeoutNs(-1);
    }

    @Test(expected = NullPointerException.class)
    public void whenAddNullPermanentListener_thenNullPointerException() {
        stm.newTransactionFactoryBuilder().addPermanentListener(null);
    }
}

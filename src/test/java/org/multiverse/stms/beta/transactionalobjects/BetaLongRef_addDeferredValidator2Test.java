package org.multiverse.stms.beta.transactionalobjects;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class BetaLongRef_addDeferredValidator2Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenSuccess() {

    }

    @Test
    @Ignore
    public void whenTransactionCommitted_thenDeadTransactionException() {

    }

    @Test
    @Ignore
    public void whenTransactionAborted_thenDeadTransactionException() {

    }

    @Test
    @Ignore
    public void whenTransactionPrepared_thenPreparedTransactionException() {
    }

    @Test
    @Ignore
    public void whenNullTransaction() {

    }

    @Test
    @Ignore
    public void whenNullValidator() {

    }
}

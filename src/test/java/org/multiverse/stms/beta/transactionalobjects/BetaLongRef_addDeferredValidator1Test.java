package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class BetaLongRef_addDeferredValidator1Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenNoTransactionAvailable_thenAddedAtomically() {

    }

    @Test
    @Ignore
    public void whenCommittedTransactionAvailable_thenAddedAtomically() {

    }

    @Test
    @Ignore
    public void whenAbortedTransactionAvailable_thenAddedAtomically() {

    }

    @Test
    @Ignore
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
    }

    @Test
    @Ignore
    public void whenNullPredicate_thenNullPointerException() {

    }

    @Test
    @Ignore
    public void whenSuccess() {
    }

}

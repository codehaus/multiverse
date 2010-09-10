package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class LongRef_commute1Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }
}

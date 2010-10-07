package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class BetaLongRef_atomicAddDeferredValidatorTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullValidator_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);

        ref.atomicAddDeferredValidator(null);
    }

    @Test
    @Ignore
    public void whenActiveTransactionAvailable_thenIgnored(){

    }

    @Test
    @Ignore
    public void whenAlreadyInList(){

    }

    @Test
    @Ignore
    public void whenSuccess(){

    }
}

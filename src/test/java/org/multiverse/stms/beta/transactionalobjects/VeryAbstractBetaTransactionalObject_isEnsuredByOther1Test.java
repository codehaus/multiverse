package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class VeryAbstractBetaTransactionalObject_isEnsuredByOther1Test {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);

        ref.isEnsuredByOther(null);
    }

    @Test
    @Ignore
    public void whenFree(){

    }

    @Test
    @Ignore
    public void whenPrivatizedBySelf(){

    }

    @Test
    @Ignore
    public void whenEnsuredBySelf(){

    }

    @Test
    @Ignore
    public void whenPrivatizedByOther(){

    }
    
    @Test
    @Ignore
    public void whenEnsuredByOther(){

    }
}

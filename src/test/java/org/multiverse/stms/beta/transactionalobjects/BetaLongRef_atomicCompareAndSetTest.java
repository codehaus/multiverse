package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class BetaLongRef_atomicCompareAndSetTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenLocked(){

    }
    
    @Test
    @Ignore
    public void whenActiveTransactionAvailable_thenIgnored(){

    }

    @Test
    @Ignore
    public void whenNoChange(){

    }

    @Test
    @Ignore
    public void whenSuccess(){

    }

    @Test
    @Ignore
    public void whenListenersAvailable(){

    }
}

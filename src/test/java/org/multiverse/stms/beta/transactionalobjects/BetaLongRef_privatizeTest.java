package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class BetaLongRef_privatizeTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void whenNoTransactionAvailable(){

    }
    
    @Test
    @Ignore
    public void whenCommittedTransactionAvailable(){

    }

    @Test
    @Ignore
    public void whenAlreadyPrivatizedBySelf() {

    }

    @Test
    @Ignore
    public void whenAlreadyEnsuredBySelf() {

    }

    @Test
    @Ignore
    public void whenFreeAndNotReadBefore() {

    }

    @Test
    @Ignore
    public void whenFreeAndReadBefore() {

    }

    @Test
    @Ignore
    public void whenFreeAndReadBeforeAndConflict() {

    }

    @Test
    @Ignore
    public void whenAlreadyPrivatizedByOther() {

    }

    @Test
    @Ignore
    public void whenAlreadyEnsuredByOther() {

    }
}

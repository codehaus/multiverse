package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionPool;

public class ReadConflictTest {
    private BetaStm stm;
    private BetaTransactionPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaTransactionPool();
    }

    @Test
    @Ignore
    public void test(){}

    @Test
    @Ignore
    public void whenAlreadyReadThenNoConflict(){

    }

    @Test
    @Ignore
    public void whenNotAttached(){

    }

    @Test
    @Ignore
    public void whenAlreadyWriteThenNoConflict(){

    }
}

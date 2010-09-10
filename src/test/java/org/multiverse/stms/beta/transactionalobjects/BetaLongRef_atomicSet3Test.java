package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransactionPool;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class BetaLongRef_atomicSet3Test {
    private BetaStm stm;
    private BetaTransactionPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaTransactionPool();
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void test(){}
}

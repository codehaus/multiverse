package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaTransactionPool;

/**
 * @author Peter Veentjer
 */
public class LongRef_atomicSet3Test {
    private BetaStm stm;
    private BetaTransactionPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaTransactionPool();
    }
}

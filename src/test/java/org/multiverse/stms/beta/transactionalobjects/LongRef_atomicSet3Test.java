package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

/**
 * @author Peter Veentjer
 */
public class LongRef_atomicSet3Test {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }
}

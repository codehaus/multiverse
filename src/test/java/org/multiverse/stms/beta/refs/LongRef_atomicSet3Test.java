package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.ObjectPool;

/**
 * @author Peter Veentjer
 */
public class LongRef_atomicSet3Test {
    private BetaStm stm;
    private ObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new ObjectPool();
    }
}

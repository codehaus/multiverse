package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRefTranlocal_evaluateCommutingFunctionsTest {

    private BetaObjectPool pool;
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void test() {
        LongRef ref = createLongRef(stm);

        //ref.openForCommute()
    }
}

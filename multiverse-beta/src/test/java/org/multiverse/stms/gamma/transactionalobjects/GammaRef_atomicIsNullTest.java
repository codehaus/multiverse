package org.multiverse.stms.gamma.transactionalobjects;

import org.junit.Before;
import org.multiverse.stms.gamma.GammaStm;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class GammaRef_atomicIsNullTest {


    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTransaction();
    }

}

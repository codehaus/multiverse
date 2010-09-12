package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmUtils;

import static org.junit.Assert.assertNull;

public class RefTranlocal_prepareForPoolingTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        BetaRef<String> ref = BetaStmUtils.newRef(stm, "peter");
        RefTranlocal tranlocal = ref.___unsafeLoad();
        tranlocal.prepareForPooling(pool);

        assertNull(tranlocal.value);
    }
}

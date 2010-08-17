package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRef_openForCommuteTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp(){
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test(){
        LongRef ref = createLongRef(stm, 100);
        LongRefTranlocal tranlocal = ref.openForCommute(pool);

        assertNotNull(tranlocal);
        assertFalse(tranlocal.isCommitted);
        assertTrue(tranlocal.isCommuting);
        assertFalse(tranlocal.isDirty);
        assertEquals(0, tranlocal.value);
        assertNull(tranlocal.read);
        assertNull(tranlocal.headCallable);
        assertFalse(tranlocal.isPermanent);
    }
}

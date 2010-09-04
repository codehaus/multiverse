package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRef_openForCommuteTest implements BetaStmConstants{
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp(){
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    @Ignore
    public void whenLocked(){

    }

    @Test
    @Ignore
    public void whenLockedBySelf(){

    }

    @Test
    @Ignore
    public void whenConstructing(){

    }

    @Test
    public void test(){
        BetaLongRef ref = createLongRef(stm, 100);
        LongRefTranlocal tranlocal = ref.___openForCommute(pool);

        assertNotNull(tranlocal);
        assertFalse(tranlocal.isCommitted);
        assertTrue(tranlocal.isCommuting);
        assertEquals(DIRTY_UNKNOWN, tranlocal.isDirty);
        assertEquals(0, tranlocal.value);
        assertNull(tranlocal.read);
        assertNull(tranlocal.headCallable);
        assertFalse(tranlocal.isPermanent);
    }
}

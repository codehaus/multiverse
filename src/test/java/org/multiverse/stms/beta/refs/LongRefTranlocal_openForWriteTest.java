package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRefTranlocal_openForWriteTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test(){
        LongRef ref = createLongRef(stm, 200);
        LongRefTranlocal committed = ref.___unsafeLoad();

        LongRefTranlocal write = committed.openForWrite(pool);
        assertNotNull(write);
        assertSame(ref, write.owner);
        assertEquals(200, write.value);
        assertFalse(write.isCommuting);
        assertFalse(write.isCommitted);
        assertFalse(write.isDirty);
        assertSame(committed, write.read);
    }
}

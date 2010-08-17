package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;

public class LongRef_openForConstructionTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LongRef ref = new LongRef(tx);
        LongRefTranlocal constructed = ref.openForConstruction(pool);

        assertNotNull(constructed);
        assertSame(ref, constructed.owner);
        assertNull(constructed.read);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isCommuting);
        assertFalse(constructed.isDirty);
        assertEquals(0, constructed.value);
    }
}

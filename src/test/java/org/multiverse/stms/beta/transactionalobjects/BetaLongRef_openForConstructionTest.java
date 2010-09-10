package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class BetaLongRef_openForConstructionTest implements BetaStmConstants{
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);
        LongRefTranlocal constructed = ref.___openForConstruction(pool);

        assertNotNull(constructed);
        assertSame(ref, constructed.owner);
        assertNull(constructed.read);
        assertFalse(constructed.isCommitted);
        assertFalse(constructed.isCommuting);
        assertEquals(DIRTY_UNKNOWN, constructed.isDirty);
        assertEquals(0, constructed.value);
    }
}

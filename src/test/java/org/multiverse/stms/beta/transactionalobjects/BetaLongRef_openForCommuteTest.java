package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class BetaLongRef_openForCommuteTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenPrepared_thenOpenForCommuteNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);

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

      @Test
    public void whenPrivatized_thenOpenForCommuteNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);
        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);

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

    @Test
    public void whenUnlocked_thenOpenForCommuteNoProblem() {
        BetaLongRef ref = newLongRef(stm, 100);
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

    @Test
    public void whenConstructing() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);

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

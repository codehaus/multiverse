package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class LongRef_constructionTest implements BetaStmConstants {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void testBasicConstructorWithoutArguments() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LongRef ref1 = new LongRef(tx);
        tx.openForConstruction(ref1, pool);
        tx.commit();

        LongRef ref2 = new LongRef();
        assertEquals(ref1.___toOrecString(), ref2.___toOrecString());

        assertNull(ref2.___getLockOwner());
        assertNotNull(ref2.___unsafeLoad());
        assertTrue(ref2.___unsafeLoad().isCommitted);
        assertFalse(ref2.___unsafeLoad().isCommuting);
        assertEquals(DIRTY_FALSE, ref2.___unsafeLoad().isDirty);
        assertEquals(0, ref2.___unsafeLoad().value);
    }

    @Test
    public void testBasicConstructorWithInitialValue() {
        BetaTransaction tx = stm.startDefaultTransaction();
        LongRef ref1 = new LongRef(tx);
        tx.openForConstruction(ref1, pool).value = 10;
        tx.commit();

        LongRef ref2 = new LongRef(10);
        assertEquals(ref1.___toOrecString(), ref2.___toOrecString());

        assertNull(ref2.___getLockOwner());
        assertNotNull(ref2.___unsafeLoad());
        assertTrue(ref2.___unsafeLoad().isCommitted);
        assertFalse(ref2.___unsafeLoad().isCommuting);
        assertEquals(DIRTY_FALSE, ref2.___unsafeLoad().isDirty);
        assertEquals(10, ref2.___unsafeLoad().value);
    }


    @Test
    public void test() {
        BetaTransaction tx = mock(BetaTransaction.class);
        LongRef ref = new LongRef(tx);

        assertSurplus(1, ref);
        assertLocked(ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertSame(tx, ref.___getLockOwner());
    }
}

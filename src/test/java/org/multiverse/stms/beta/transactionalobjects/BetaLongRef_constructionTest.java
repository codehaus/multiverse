package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.BetaStmConstants;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.stms.beta.orec.OrecTestUtils.*;

public class BetaLongRef_constructionTest implements BetaStmConstants {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void testBasicConstructorWithoutArguments() {
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref1 = new BetaLongRef(tx);
        tx.openForConstruction(ref1);
        tx.commit();

        BetaLongRef ref2 = new BetaLongRef(stm);
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
        BetaLongRef ref1 = new BetaLongRef(tx);
        tx.openForConstruction(ref1).value = 10;
        tx.commit();

        BetaLongRef ref2 = new BetaLongRef(stm,10);
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
        BetaTransaction tx = stm.startDefaultTransaction();
        BetaLongRef ref = new BetaLongRef(tx);

        assertSurplus(1, ref);
        assertLocked(ref);
        assertReadonlyCount(0, ref);
        assertUpdateBiased(ref);
        assertNull(ref.___unsafeLoad());
        assertSame(tx, ref.___getLockOwner());
    }
}

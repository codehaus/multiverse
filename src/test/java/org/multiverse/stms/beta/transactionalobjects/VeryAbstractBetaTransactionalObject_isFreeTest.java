package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class VeryAbstractBetaTransactionalObject_isFreeTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = new BetaStm();
    }

    @Test
    public void whenFree_thenTrue() {
        BetaLongRef ref = newLongRef(stm);
        assertTrue(ref.isFree());
    }

    @Test
    public void whenPrivatized_thenFalse() {
        BetaLongRef ref = newLongRef(stm);
        ref.privatize(stm.startDefaultTransaction());

        assertFalse(ref.isFree());
    }

    @Test
    public void whenEnsured_thenFalse() {
        BetaLongRef ref = newLongRef(stm);
        ref.ensure(stm.startDefaultTransaction());

        assertFalse(ref.isFree());
    }
}

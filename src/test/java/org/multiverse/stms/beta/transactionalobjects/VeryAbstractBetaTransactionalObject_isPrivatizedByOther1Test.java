package org.multiverse.stms.beta.transactionalobjects;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class VeryAbstractBetaTransactionalObject_isPrivatizedByOther1Test {

     private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullTransaction_thenNullPointerException() {
        BetaLongRef ref = newLongRef(stm);

        ref.isPrivatizedByOther(null);
    }

    @Test
    public void whenFree(){
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isPrivatizedByOther(tx);

        assertFalse(result);
    }

    @Test
    public void whenPrivatizedBySelf(){
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.privatize(tx);
        boolean result = ref.isPrivatizedByOther(tx);

        assertFalse(result);
    }

    @Test
    public void whenEnsuredBySelf(){
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.ensure(tx);
        boolean result = ref.isPrivatizedByOther(tx);

        assertFalse(result);
    }

    @Test
    public void whenPrivatizedByOther(){
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.privatize(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isPrivatizedByOther(tx);

        assertTrue(result);
    }

    @Test
    public void whenEnsuredByOther(){
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref.ensure(otherTx);

        BetaTransaction tx = stm.startDefaultTransaction();
        boolean result = ref.isPrivatizedByOther(tx);

        assertFalse(result);
    }
}

package org.multiverse.stms.beta.refs;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.LockStatus;
import org.multiverse.api.Transaction;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactions.LeanMonoBetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class LongRef_getLockStatusTest {

    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test(expected = NullPointerException.class)
    public void whenNull_thenNullPointerException() {
        LongRef ref = createLongRef(stm);
        ref.getLockStatus(null);
    }

    @Test
    public void whenFree() {
        LongRef ref = createLongRef(stm);

        Transaction tx = mock(Transaction.class);
        LockStatus result = ref.getLockStatus(tx);
        assertEquals(LockStatus.Free, result);
    }

    @Test
    public void whenLockedByOther() {
        LongRef ref = createLongRef(stm);

        LeanMonoBetaTransaction otherTx = new LeanMonoBetaTransaction(stm);
        otherTx.openForRead(ref, true, pool);

        Transaction tx = mock(Transaction.class);

        LockStatus result = ref.getLockStatus(tx);
        assertEquals(LockStatus.LockedByOther, result);
    }

    @Test
    public void whenLockedBySelf() {
        LongRef ref = createLongRef(stm);

        LeanMonoBetaTransaction tx = new LeanMonoBetaTransaction(stm);
        tx.openForRead(ref, true, pool);


        LockStatus result = ref.getLockStatus(tx);
        assertEquals(LockStatus.LockedBySelf, result);
    }
}

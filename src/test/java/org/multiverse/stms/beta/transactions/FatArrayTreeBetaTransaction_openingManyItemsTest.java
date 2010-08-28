package org.multiverse.stms.beta.transactions;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactionalobjects.LongRefTranlocal;
import org.multiverse.stms.beta.transactionalobjects.Tranlocal;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertActive;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class FatArrayTreeBetaTransaction_openingManyItemsTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void whenReadonly() {
        whenManyItems(true);
    }

    @Test
    public void whenUpdate() {
        whenManyItems(false);
    }

    public void whenManyItems(boolean reading) {
        FatArrayTreeBetaTransaction tx = new FatArrayTreeBetaTransaction(stm);

        int refCount = 10000;
        BetaLongRef[] refs = new BetaLongRef[refCount];
        LongRefTranlocal[] tranlocals = new LongRefTranlocal[refCount];
        for (int k = 0; k < refCount; k++) {
            BetaLongRef ref = createLongRef(stm);
            refs[k] = ref;
            tranlocals[k] = reading ? tx.openForWrite(ref, false, pool) : tx.openForWrite(ref, false, pool);
        }

        assertEquals(refCount, tx.size());

        System.out.println("everything inserted");
        System.out.println("usage percentage: " + (100 * tx.getUsage()));

        for (int k = 0; k < refCount; k++) {
            BetaLongRef ref = refs[k];
            Tranlocal found = reading ? tx.openForWrite(ref, false, pool) : tx.openForWrite(ref, false, pool);
            assertSame(ref, found.owner);
            assertSame("tranlocal is incorrect at " + k, tranlocals[k], found);
        }

        assertActive(tx);
    }
}

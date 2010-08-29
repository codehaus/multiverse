package org.multiverse.stms.beta.integrationtest.liveness;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.stms.beta.BetaObjectPool;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertAborted;
import static org.multiverse.stms.beta.BetaStmUtils.createLongRef;

public class DeadLockTest {
    private BetaStm stm;
    private BetaObjectPool pool;

    @Before
    public void setUp() {
        stm = new BetaStm();
        pool = new BetaObjectPool();
    }

    @Test
    public void test() {
        BetaLongRef ref1 = createLongRef(stm);
        BetaLongRef ref2 = createLongRef(stm);

        BetaTransaction tx1 = stm.startDefaultTransaction();
        BetaTransaction tx2 = stm.startDefaultTransaction();

        tx1.openForWrite(ref1, true, pool).value++;
        tx2.openForWrite(ref2, true, pool).value++;

        try{
            tx1.openForWrite(ref2, true, pool);
            fail();
        }catch(ReadConflict expected){
        }

        assertAborted(tx1);

        tx2.openForWrite(ref1, true, pool).value++;
        tx2.commit();

        assertEquals(1, ref1.atomicGet());
        assertEquals(1, ref2.atomicGet());
    }
}

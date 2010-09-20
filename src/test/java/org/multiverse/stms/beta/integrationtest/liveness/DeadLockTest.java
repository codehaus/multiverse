package org.multiverse.stms.beta.integrationtest.liveness;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class DeadLockTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void test() {
        BetaLongRef ref1 = newLongRef(stm);
        BetaLongRef ref2 = newLongRef(stm);

        BetaTransaction tx1 = stm.startDefaultTransaction();
        BetaTransaction tx2 = stm.startDefaultTransaction();

        tx1.openForWrite(ref1, true).value++;
        tx2.openForWrite(ref2, true).value++;

        try{
            tx1.openForWrite(ref2, true);
            fail();
        }catch(ReadWriteConflict expected){
        }

        assertIsAborted(tx1);

        tx2.openForWrite(ref1, true).value++;
        tx2.commit();

        assertEquals(1, ref1.atomicGet());
        assertEquals(1, ref2.atomicGet());
    }
}

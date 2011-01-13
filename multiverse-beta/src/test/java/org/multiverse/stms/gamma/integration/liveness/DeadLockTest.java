package org.multiverse.stms.gamma.integration.liveness;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.gamma.GammaConstants;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class DeadLockTest implements GammaConstants {
    private GammaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (GammaStm) getGlobalStmInstance();
    }

    @Test
    public void test() {
        GammaLongRef ref1 = new GammaLongRef(stm);
        GammaLongRef ref2 = new GammaLongRef(stm);

        GammaTransaction tx1 = stm.startDefaultTransaction();
        GammaTransaction tx2 = stm.startDefaultTransaction();

        tx1.openForWrite(ref1, LOCKMODE_COMMIT).long_value++;
        tx2.openForWrite(ref2, LOCKMODE_COMMIT).long_value++;

        try {
            tx1.openForWrite(ref2, LOCKMODE_COMMIT);
            fail();
        } catch (ReadWriteConflict expected) {
        }

        assertIsAborted(tx1);

        tx2.openForWrite(ref1, LOCKMODE_COMMIT).long_value++;
        tx2.commit();

        assertEquals(1, ref1.atomicGet());
        assertEquals(1, ref2.atomicGet());
    }
}

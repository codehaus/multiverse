package org.multiverse.stms.beta.integrationtest.isolation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.stms.beta.BetaStmUtils.newLongRef;

public class ReadConflictTest {
    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenAlreadyReadThenNoConflict() {
        BetaLongRef ref = newLongRef(stm, 5);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);

        ref.atomicIncrementAndGet(1);

        long result = ref.get(tx);

        assertEquals(5, result);
        tx.commit();

        assertIsCommitted(tx);
    }

    @Test
    public void testCausalConsistency() {
        BetaLongRef ref1 = newLongRef(stm, 0);
        BetaLongRef ref2 = newLongRef(stm, 0);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref1.get(tx);

        BetaTransaction otherTx = stm.startDefaultTransaction();
        ref1.set(otherTx, 1);
        ref2.set(otherTx, 1);
        otherTx.commit();

        try {
            ref2.get(tx);
            fail();
        } catch (ReadConflict expected) {
        }

        assertIsAborted(tx);
    }
}

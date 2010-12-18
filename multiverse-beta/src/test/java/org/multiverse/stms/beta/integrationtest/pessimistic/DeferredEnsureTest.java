package org.multiverse.stms.beta.integrationtest.pessimistic;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.exceptions.ReadWriteConflict;
import org.multiverse.stms.beta.BetaStm;
import org.multiverse.stms.beta.transactionalobjects.BetaLongRef;
import org.multiverse.stms.beta.transactions.BetaTransaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.stms.beta.BetaStmTestUtils.assertRefHasNoLocks;
import static org.multiverse.stms.beta.BetaStmTestUtils.newLongRef;

public class DeferredEnsureTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    @Ignore
    public void whenEnsureWrites(){

    }

    @Test
    @Ignore
    public void whenPrivatizeWrites(){

    }

    @Test
    public void whenOnlyReadsThenIgnored() {
        BetaLongRef ref = newLongRef(stm);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref.get(tx);
        ref.deferredEnsure(tx);

        ref.atomicIncrementAndGet(1);

        tx.commit();

        assertIsCommitted(tx);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenUpdateTransactionButNoConflictOnRead_thenSuccess() {
        long initialValue = 10;
        BetaLongRef ref1 = newLongRef(stm, initialValue);
        BetaLongRef ref2 = newLongRef(stm, initialValue);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref1.get(tx);
        ref1.deferredEnsure(tx);
        ref2.increment(tx);

        tx.commit();
        assertIsCommitted(tx);
        assertEquals(initialValue, ref1.atomicGet());
        assertEquals(initialValue + 1, ref2.atomicGet());
    }

    @Test
    public void whenUpdateTransactionAndConflictOnRead_thenReadWriteConflict() {
        long initialValue = 10;
        BetaLongRef ref1 = newLongRef(stm, initialValue);
        BetaLongRef ref2 = newLongRef(stm, initialValue);

        BetaTransaction tx = stm.startDefaultTransaction();
        ref1.get(tx);
        ref1.deferredEnsure(tx);
        ref2.increment(tx);

        ref1.atomicIncrementAndGet(1);

        try {
            tx.commit();
            fail();
        } catch (ReadWriteConflict expected) {

        }
        assertIsAborted(tx);
        assertEquals(initialValue+1, ref1.atomicGet());
        assertEquals(initialValue, ref2.atomicGet());
    }
}

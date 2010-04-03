package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.api.latches.CheapLatch;
import org.multiverse.api.latches.Latch;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.alpha.AlphaTestUtils.startTrackingUpdateTransaction;

public class RegisterRetryListenerTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
    }

    public Transaction startUpdateTransaction() {
        Transaction t = startTrackingUpdateTransaction(stm);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testNothingRead() {
        Transaction tx = startUpdateTransaction();
        long startVersion = stm.getVersion();
        Latch latch = new CheapLatch();
        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertEquals(startVersion, stm.getVersion());
        assertFalse(latch.isOpen());
        assertIsActive(tx);
    }

    @Test
    public void testOnlyNewOnesAttached() {
        Transaction tx = startUpdateTransaction();
        TransactionalInteger ref = new TransactionalInteger(0);
        Latch latch = new CheapLatch();

        try {
            tx.registerRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertIsActive(tx);
        assertFalse(latch.isOpen());
    }

    @Test
    public void test() {
        Transaction tx1 = startUpdateTransaction();
        TransactionalInteger ref = new TransactionalInteger(10);
        tx1.commit();

        Transaction tx2 = startUpdateTransaction();
        int result = ref.get();
        Latch latch = new CheapLatch();
        tx2.registerRetryLatch(latch);

        assertIsActive(tx2);
        assertFalse(latch.isOpen());
    }
}

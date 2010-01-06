package org.multiverse.stms.alpha;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionStatus;
import org.multiverse.api.exceptions.NoRetryPossibleException;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

public class RegisterRetryListenerTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
    }

    public Transaction startUpdateTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testNothingRead() {
        Transaction t = startUpdateTransaction();
        long startVersion = stm.getTime();
        Latch latch = new CheapLatch();
        try {
            t.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertEquals(startVersion, stm.getTime());
        assertFalse(latch.isOpen());
        assertEquals(TransactionStatus.aborted, t.getStatus());
    }

    @Test
    public void testOnlyNewOnesAttached() {
        Transaction t = startUpdateTransaction();
        IntRef intValue = new IntRef(0);
        Latch latch = new CheapLatch();

        try {
            t.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (NoRetryPossibleException ex) {
        }

        assertIsAborted(t);
        assertFalse(latch.isOpen());
    }

    @Test
    public void test() {
        Transaction t1 = startUpdateTransaction();
        IntRef intValue = new IntRef(10);
        t1.commit();

        Transaction t2 = startUpdateTransaction();
        int result = intValue.get();
        Latch latch = new CheapLatch();
        t2.abortAndRegisterRetryLatch(latch);

        assertIsAborted(t2);
        assertFalse(latch.isOpen());
    }
}

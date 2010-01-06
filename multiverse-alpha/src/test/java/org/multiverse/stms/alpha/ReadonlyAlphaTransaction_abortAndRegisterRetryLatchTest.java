package org.multiverse.stms.alpha;

import org.junit.After;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.ReadonlyException;
import org.multiverse.utils.latches.CheapLatch;
import org.multiverse.utils.latches.Latch;

/**
 * @author Peter Veentjer
 */
public class ReadonlyAlphaTransaction_abortAndRegisterRetryLatchTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = AlphaStm.createDebug();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void after() {
        setThreadLocalTransaction(null);
    }

    public AlphaTransaction startReadonlyTransaction() {
        AlphaTransaction t = stm.startReadOnlyTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void callFailsIfTransactionIsStarted() {
        Transaction t = startReadonlyTransaction();
        Latch latch = new CheapLatch();

        try {
            t.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (ReadonlyException ex) {
        }

        assertFalse(latch.isOpen());
        assertIsAborted(t);
    }

    @Test
    public void callFailsIfTransactionIsCommitted() {
        Transaction t = startReadonlyTransaction();
        t.commit();

        Latch latch = new CheapLatch();
        try {
            t.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsCommitted(t);
        assertFalse(latch.isOpen());
    }

    @Test
    public void callFailsIfTransactionIsAborted() {
        Transaction t = startReadonlyTransaction();
        t.abort();

        Latch latch = new CheapLatch();
        try {
            t.abortAndRegisterRetryLatch(latch);
            fail();
        } catch (DeadTransactionException ex) {
        }

        assertIsAborted(t);
        assertFalse(latch.isOpen());
    }
}

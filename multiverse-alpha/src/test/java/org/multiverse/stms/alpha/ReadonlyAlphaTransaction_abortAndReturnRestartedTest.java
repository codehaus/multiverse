package org.multiverse.stms.alpha;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;

/**
 * @author Peter Veentjer
 */
public class ReadonlyAlphaTransaction_abortAndReturnRestartedTest {

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
    public void callActiveTransaction() {
        AlphaTransaction t = startReadonlyTransaction();

        long version = stm.getTime();
        Transaction result = t.abortAndReturnRestarted();
        assertSame(t, result);
        assertEquals(version, stm.getTime());
        assertIsActive(t);
    }

    @Test
    public void callAbortedTransaction() {
        AlphaTransaction t = startReadonlyTransaction();
        t.abort();

        long version = stm.getTime();
        Transaction result = t.abortAndReturnRestarted();
        assertSame(t, result);
        assertEquals(version, stm.getTime());
        assertIsActive(t);
    }

    @Test
    public void resetOnCommittedTransaction() {
        AlphaTransaction t = startReadonlyTransaction();
        t.commit();

        long version = stm.getTime();
        Transaction result = t.abortAndReturnRestarted();
        assertSame(t, result);
        assertEquals(version, stm.getTime());
        assertIsActive(t);
    }
}

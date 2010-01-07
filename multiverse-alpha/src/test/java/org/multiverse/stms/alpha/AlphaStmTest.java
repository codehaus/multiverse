package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.transactions.update.AbstractUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.GrowingUpdateAlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaStmTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void testDefaultTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder().build().start();
        assertTrue(t instanceof AbstractUpdateAlphaTransaction);

        //parameters should be checked
        testIncomplete();
    }

    @Test
    public void testDefaultUpdateTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder().setReadonly(false).build().start();
        assertTrue(t instanceof GrowingUpdateAlphaTransaction);

        assertFalse(t.getConfig().isReadonly());
        assertTrue(t.getConfig().automaticReadTracking());
        assertFalse(t.getConfig().detectWriteSkew());
        assertEquals(1000, t.getConfig().getMaxRetryCount());
        assertFalse(t.getConfig().isInterruptible());
    }

    @Test
    public void test() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setAutomaticReadTracking(true).build().start();

        assertTrue(t instanceof AbstractUpdateAlphaTransaction);

        assertFalse(t.getConfig().isReadonly());
        assertTrue(t.getConfig().automaticReadTracking());
        assertFalse(t.getConfig().detectWriteSkew());
        assertEquals(1000, t.getConfig().getMaxRetryCount());
        assertFalse(t.getConfig().isInterruptible());
    }

    @Test
    public void testNonTrackingUpdate() {
        Transaction t = stm.getTransactionFactoryBuilder().setReadonly(false).setAutomaticReadTracking(true).build()
                .start();
        assertTrue(t instanceof GrowingUpdateAlphaTransaction);
    }
}

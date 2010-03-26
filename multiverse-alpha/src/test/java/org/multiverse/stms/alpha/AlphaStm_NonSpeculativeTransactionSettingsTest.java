package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.transactions.update.AbstractUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MapUpdateAlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaStm_NonSpeculativeTransactionSettingsTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void testDefaultTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .build()
                .start();

        assertTrue(t instanceof AbstractUpdateAlphaTransaction);

        //parameters should be checked
        testIncomplete();
    }

    @Test
    public void testDefaultUpdateTransaction() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .build()
                .start();
        assertTrue(t instanceof MapUpdateAlphaTransaction);

        assertFalse(t.getConfiguration().isReadonly());
        assertFalse(t.getConfiguration().automaticReadTracking());
        assertTrue(t.getConfiguration().allowWriteSkewProblem());
        assertEquals(1000, t.getConfiguration().getMaxRetryCount());
        assertFalse(t.getConfiguration().isInterruptible());
    }

    @Test
    public void test() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setAutomaticReadTracking(true)
                .build()
                .start();

        assertTrue(t instanceof AbstractUpdateAlphaTransaction);

        assertFalse(t.getConfiguration().isReadonly());
        assertTrue(t.getConfiguration().automaticReadTracking());
        assertTrue(t.getConfiguration().allowWriteSkewProblem());
        assertEquals(1000, t.getConfiguration().getMaxRetryCount());
        assertFalse(t.getConfiguration().isInterruptible());
    }

    @Test
    public void testNonTrackingUpdate() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .setAutomaticReadTracking(true)
                .build()
                .start();

        assertTrue(t instanceof MapUpdateAlphaTransaction);
    }

    @Test(expected = IllegalStateException.class)
    public void whenUpdateTransactionWithWriteSkewDetectionAndNoAutomaticReadTracking_thenIllegalStateException() {
        stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .setAutomaticReadTracking(false)
                .setAllowWriteSkewProblem(false)
                .build();
    }

    @Test
    public void whenReadonlyTransactionWithWriteSkewDetectionAndNoAutomaticReadTracking_thenIgnore() {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(true)
                .setAutomaticReadTracking(false)
                .setAllowWriteSkewProblem(true)
                .build();
    }
}

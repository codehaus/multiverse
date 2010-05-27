package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.api.TransactionFactory;
import org.multiverse.stms.alpha.transactions.readonly.NonTrackingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.MapUpdateAlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class AlphaStm_NonSpeculativeTransactionSettingsTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
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
        assertFalse(t.getConfiguration().isReadTrackingEnabled());
        assertTrue(t.getConfiguration().isWriteSkewAllowed());
        assertEquals(1000, t.getConfiguration().getMaxRetries());
        assertFalse(t.getConfiguration().isInterruptible());
    }

    @Test
    public void testDefault() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .build()
                .start();

        assertTrue(t instanceof NonTrackingReadonlyAlphaTransaction);

        assertTrue(t.getConfiguration().isReadonly());
        assertFalse(t.getConfiguration().isReadTrackingEnabled());
        assertEquals(1000, t.getConfiguration().getMaxRetries());
        assertFalse(t.getConfiguration().isInterruptible());
    }

    @Test
    public void testNonTrackingUpdate() {
        Transaction t = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .setReadTrackingEnabled(true)
                .build()
                .start();

        assertTrue(t instanceof MapUpdateAlphaTransaction);
    }

    @Test(expected = IllegalStateException.class)
    public void whenUpdateTransactionWithWriteSkewDetectionAndNoAutomaticReadTracking_thenIllegalStateException() {
        stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(false)
                .setReadTrackingEnabled(false)
                .setWriteSkewAllowed(false)
                .build();
    }

    @Test
    public void whenReadonlyTransactionWithWriteSkewDetectionAndNoAutomaticReadTracking_thenIgnore() {
        TransactionFactory txFactory = stm.getTransactionFactoryBuilder()
                .setSpeculativeConfigurationEnabled(false)
                .setReadonly(true)
                .setReadTrackingEnabled(false)
                .setWriteSkewAllowed(true)
                .build();
    }
}

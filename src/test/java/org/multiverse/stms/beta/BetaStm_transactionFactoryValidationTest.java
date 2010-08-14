package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.PessimisticLockLevel;

import static org.junit.Assert.fail;

public class BetaStm_transactionFactoryValidationTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenBlockingEnabled_thenAutomaticReadTrackingShouldBeEnabled() {
        BetaTransactionFactoryBuilder builder = stm.getTransactionFactoryBuilder()
                .setReadTrackingEnabled(false)
                .setBlockingAllowed(true);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void whenWriteSkewAllowed_thenAutomaticReadTrackingShouldBeEnabled() {
        BetaTransactionFactoryBuilder builder = stm.getTransactionFactoryBuilder()
                .setReadonly(false)
                .setReadTrackingEnabled(false)
                .setWriteSkewAllowed(false);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) {
        }
    }

    @Test
    public void whenWriteSkewAllowedAndReadonly_thenThenAutomaticReadTrackingDoesntMatter() {
        whenWriteSkewAllowedAndReadonly(true);
        whenWriteSkewAllowedAndReadonly(false);
    }

    private void whenWriteSkewAllowedAndReadonly(boolean readTrackingEnabled) {
        BetaTransactionFactoryBuilder builder = stm.getTransactionFactoryBuilder()
                .setBlockingAllowed(false)
                .setReadonly(true)
                .setReadTrackingEnabled(readTrackingEnabled)
                .setWriteSkewAllowed(false);

        builder.build();
    }


    @Test
    public void whenPessimisticLockLevelIsRead_thenAutomaticReadTrackingShouldBeEnabled() {
        BetaTransactionFactoryBuilder builder = stm.getTransactionFactoryBuilder()
                .setReadTrackingEnabled(false)
                .setPessimisticLockLevel(PessimisticLockLevel.Read);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) {
        }
    }
}

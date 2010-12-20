package org.multiverse.stms.beta;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.IsolationLevel;
import org.multiverse.api.LockLevel;

import static org.junit.Assert.fail;

public class BetaStm_transactionFactoryValidationTest {

    private BetaStm stm;

    @Before
    public void setUp() {
        stm = new BetaStm();
    }

    @Test
    public void whenBlockingEnabled_thenAutomaticReadTrackingShouldBeEnabled() {
        BetaTransactionFactoryBuilder builder = stm.createTransactionFactoryBuilder()
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
        BetaTransactionFactoryBuilder builder = stm.createTransactionFactoryBuilder()
                .setReadonly(false)
                .setReadTrackingEnabled(false)
                .setIsolationLevel(IsolationLevel.Serializable);

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
        BetaTransactionFactoryBuilder builder = stm.createTransactionFactoryBuilder()
                .setBlockingAllowed(false)
                .setReadonly(true)
                .setReadTrackingEnabled(readTrackingEnabled)
                .setIsolationLevel(IsolationLevel.Serializable);

        builder.build();
    }


    @Test
    public void whenLockLevelIsRead_thenAutomaticReadTrackingShouldBeEnabled() {
        BetaTransactionFactoryBuilder builder = stm.createTransactionFactoryBuilder()
                .setReadTrackingEnabled(false)
                .setLockLevel(LockLevel.PrivatizeReads);

        try {
            builder.build();
            fail();
        } catch (IllegalStateException expected) {
        }
    }
}

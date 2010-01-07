package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.annotations.TransactionalConstructor;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalConstructor_settingsTest {

    @Test
    public void readonlySettings() {
        new ReadonlyExplicitlyEnabled();
        new ReadonlyExplicitlyDisabled();
    }

    public class ReadonlyExplicitlyEnabled {

        @TransactionalConstructor(readonly = true) ReadonlyExplicitlyEnabled() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().isReadonly());
        }
    }

    public class ReadonlyExplicitlyDisabled {

        @TransactionalConstructor(readonly = false) ReadonlyExplicitlyDisabled() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().isReadonly());
        }
    }


    @Test
    public void defaultSettings() {
        new DefaultSettingsTransactionalConstructor();
    }

    public class DefaultSettingsTransactionalConstructor {

        @TransactionalConstructor
        public DefaultSettingsTransactionalConstructor() {
            Transaction tx = getThreadLocalTransaction();

            assertFalse(tx.getConfig().isReadonly());
            assertEquals(0, tx.getConfig().getMaxRetryCount());
            assertTrue(tx.getConfig().automaticReadTracking());
            assertFalse(tx.getConfig().detectWriteSkew());
        }
    }

    //=============================================

    @Test
    public void readTracking() {
        new DisabledReadTracking();
        new EnabledReadTracking();
    }

    public class DisabledReadTracking {

        @TransactionalConstructor(automaticReadTracking = false)
        public DisabledReadTracking() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().automaticReadTracking());
        }
    }

    public class EnabledReadTracking {

        @TransactionalConstructor(automaticReadTracking = true)
        public EnabledReadTracking() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().automaticReadTracking());
        }
    }

    // ============================

    @Test
    public void detectWriteSkew() {
        new DetectWriteSkewDisabled();
        new DetectWriteSkewEnabled();
    }

    public class DetectWriteSkewDisabled {

        @TransactionalConstructor(detectWriteSkew = false)
        public DetectWriteSkewDisabled() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().detectWriteSkew());
        }
    }

    public class DetectWriteSkewEnabled {

        @TransactionalConstructor(detectWriteSkew = true)
        public DetectWriteSkewEnabled() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().detectWriteSkew());
        }
    }

}

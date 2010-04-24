package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Test;
import org.multiverse.annotations.TransactionalConstructor;
import org.multiverse.api.Transaction;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalConstructor_settingsTest {

    @Test
    public void readonlySettings() {
        new ReadonlyExplicitlyEnabled();
        new ReadonlyExplicitlyDisabled();
    }

    public class ReadonlyExplicitlyEnabled {

        @TransactionalConstructor(readonly = true)
        ReadonlyExplicitlyEnabled() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isReadonly());
        }
    }

    public class ReadonlyExplicitlyDisabled {

        @TransactionalConstructor(readonly = false)
        ReadonlyExplicitlyDisabled() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isReadonly());
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

            assertFalse(tx.getConfiguration().isReadonly());
            assertEquals(0, tx.getConfiguration().getMaxRetries());
            assertFalse(tx.getConfiguration().isReadTrackingEnabled());
            assertTrue(tx.getConfiguration().isWriteSkewAllowed());
        }
    }

    //=============================================

    @Test
    public void readTracking() {
        new DisabledReadTracking();
        new EnabledReadTracking();
    }

    public class DisabledReadTracking {

        @TransactionalConstructor(trackReads = false)
        public DisabledReadTracking() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isReadTrackingEnabled());
        }
    }

    public class EnabledReadTracking {

        @TransactionalConstructor(trackReads = true)
        public EnabledReadTracking() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isReadTrackingEnabled());
        }
    }

    // ============================

    @Test
    public void allowWriteSkew() {
        new disallowedWriteSkew();
        new allowedWriteSkew();
    }

    public class disallowedWriteSkew {

        @TransactionalConstructor(trackReads = true, writeSkew = false)
        public disallowedWriteSkew() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isWriteSkewAllowed());
        }
    }

    public class allowedWriteSkew {

        @TransactionalConstructor(writeSkew = true)
        public allowedWriteSkew() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isWriteSkewAllowed());
        }
    }

}

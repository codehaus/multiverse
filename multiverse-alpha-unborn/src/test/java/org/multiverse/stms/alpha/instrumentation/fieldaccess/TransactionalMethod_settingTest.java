package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.update.MonoUpdateAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.UpdateConfiguration;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertInstanceOf;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * A Test that checks if the configuration of the Transaction is correct if the TransactionalMethod and
 * TransactionalObject annotations are used.
 *
 * @author Peter Veentjer.
 */
public class TransactionalMethod_settingTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void retryCount() {
        ObjectWithRetryCount o = new ObjectWithRetryCount();
        AtomicInteger tryCounter = new AtomicInteger();
        try {
            o.doSomething(tryCounter);
            fail();
        } catch (TooManyRetriesException expected) {
        }

        assertEquals(51 + 1, tryCounter.get());
    }

    @TransactionalObject
    public static class ObjectWithRetryCount {

        private int x;

        public ObjectWithRetryCount() {
            x = 0;
        }

        @TransactionalMethod(maxRetryCount = 51)
        public void doSomething(AtomicInteger tryCounter) {
            //force the loadForRead so that the retry doesn't find an empty transaction
            int b = x;

            tryCounter.incrementAndGet();
            throw new OldVersionNotFoundReadConflict();
        }
    }

    @Test
    public void testAllowWriteSkewProblem() {
        ObjectWithPreventWriteSkew object = new ObjectWithPreventWriteSkew();

        assertFalse(object.updateWithDisallowedWriteSkewProblem());
        assertTrue(object.updateDefaultMethod());
        assertTrue(object.updateWithAllowedWriteSkewProblem());
        assertFalse(object.updateWithDisallowedWriteSkewProblemAndDefaultAutomaticReadTracking());
    }


    @TransactionalObject
    public static class ObjectWithPreventWriteSkew {

        private int x;

        public ObjectWithPreventWriteSkew() {
            x = 0;
        }

        @TransactionalMethod(readonly = false, writeSkewProblemAllowed = true)
        public boolean updateWithAllowedWriteSkewProblem() {
            //force the loadForRead so that the retry doesn't find an empty transaction
            int b = x;

            return getThreadLocalTransaction().getConfiguration().isWriteSkewProblemAllowed();
        }

        @TransactionalMethod(readonly = false, writeSkewProblemAllowed = false, trackReads = true)
        public boolean updateWithDisallowedWriteSkewProblem() {
            //Configuration = getThreadLocalTransaction().getConfiguration(); 

            //force the loadForRead so that the retry doesn't find an empty transaction
            int b = x;

            return getThreadLocalTransaction().getConfiguration().isWriteSkewProblemAllowed();
        }

        @TransactionalMethod(readonly = false, writeSkewProblemAllowed = false)
        public boolean updateWithDisallowedWriteSkewProblemAndDefaultAutomaticReadTracking() {
            assertTrue(getThreadLocalTransaction().getConfiguration().isReadTrackingEnabled());

            //force the loadForRead so that the retry doesn't find an empty transaction
            int b = x;

            return getThreadLocalTransaction().getConfiguration().isWriteSkewProblemAllowed();
        }

        @TransactionalMethod(readonly = false)
        public boolean updateDefaultMethod() {
            //force the loadForRead so that the retry doesn't find an empty transaction
            int b = x;

            Transaction tx = getThreadLocalTransaction();

            return getThreadLocalTransaction().getConfiguration().isWriteSkewProblemAllowed();
        }
    }

    @Test
    public void testReadonly() {
        ObjectWithReadonly o = new ObjectWithReadonly();
        o.defaultMethod();
        o.explicitReadonlyMethod();
        o.explicitUpdateMethod();
    }

    @TransactionalObject
    public static class ObjectWithReadonly {

        private int x;

        public ObjectWithReadonly() {
            x = 0;
        }

        public void defaultMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isReadonly());
        }

        @TransactionalMethod(readonly = true)
        public void explicitReadonlyMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isReadonly());
        }

        @TransactionalMethod(readonly = false)
        public void explicitUpdateMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isReadonly());
        }
    }

    @Test
    public void testInterruptible() throws InterruptedException {
        ObjectWithInterruptible o = new ObjectWithInterruptible();
        o.defaultMethodIsNotInterruptible();
        o.defaultMethodWithEmptyTransactionalMethodIsNotInterruptible();
        o.withInterruptibleSet();
        o.withNoTransactionalMethodButThrowingInterruptibleException();
        o.withEmptyTransactionalMethodException();
    }

    @TransactionalObject
    public static class ObjectWithInterruptible {

        private int value;

        public ObjectWithInterruptible() {
            value = 0;
        }

        public void defaultMethodIsNotInterruptible() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isInterruptible());
        }

        @TransactionalMethod
        public void defaultMethodWithEmptyTransactionalMethodIsNotInterruptible() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isInterruptible());
        }

        @TransactionalMethod(interruptible = true)
        public void withInterruptibleSet() throws InterruptedException {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isInterruptible());
        }

        @TransactionalMethod
        public void withEmptyTransactionalMethodException() throws InterruptedException {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isInterruptible());
        }

        public void withNoTransactionalMethodButThrowingInterruptibleException() throws InterruptedException {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isInterruptible());
        }
    }

    @Test
    public void automaticReadTracking() {
        AutomaticReadTracking m = new AutomaticReadTracking();
        //m.defaultMethod();
        //m.readonlyMethod();
        //m.readonlyMethodWithReadTrackingDisabled();
        //m.readonlyMethodWithReadTrackingEnabled();
        m.defaultUpdateMethod();
        //m.updateMethodWithReadTrackingDisabled();
        //m.updateMethodWithReadTrackingEnabled();
    }

    @TransactionalObject
    public static class AutomaticReadTracking {

        private int value;

        public void defaultMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isReadTrackingEnabled());
        }

        @TransactionalMethod(readonly = true)
        public void readonlyMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isReadTrackingEnabled());
        }

        @TransactionalMethod(readonly = true, trackReads = false)
        public void readonlyMethodWithReadTrackingDisabled() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isReadTrackingEnabled());
        }

        @TransactionalMethod(readonly = true, trackReads = true)
        public void readonlyMethodWithReadTrackingEnabled() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isReadTrackingEnabled());
        }

        @TransactionalMethod(readonly = false)
        public void defaultUpdateMethod() {
            Transaction tx = getThreadLocalTransaction();
            UpdateConfiguration config = (UpdateConfiguration) tx.getConfiguration();
            System.out.println(config.speculativeConfiguration);
            assertInstanceOf(tx, MonoUpdateAlphaTransaction.class);

            assertFalse(tx.getConfiguration().isReadTrackingEnabled());
        }

        @TransactionalMethod(readonly = false, trackReads = false)
        public void updateMethodWithReadTrackingDisabled() {
            Transaction tx = getThreadLocalTransaction();

            assertFalse(tx.getConfiguration().isReadTrackingEnabled());
        }

        @TransactionalMethod(readonly = false, trackReads = true)
        public void updateMethodWithReadTrackingEnabled() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfiguration().isReadTrackingEnabled());
        }
    }
}




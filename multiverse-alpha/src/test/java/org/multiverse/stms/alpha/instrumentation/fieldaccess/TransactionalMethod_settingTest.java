package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.api.exceptions.OldVersionNotFoundReadConflict;
import org.multiverse.api.exceptions.TooManyRetriesException;
import org.multiverse.stms.alpha.AlphaStm;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
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
    }


    @TransactionalObject
    public static class ObjectWithPreventWriteSkew {

        private int x;

        public ObjectWithPreventWriteSkew() {
            x = 0;
        }

        @TransactionalMethod(allowWriteSkewProblem = true)
        public boolean updateWithAllowedWriteSkewProblem() {
            //force the loadForRead so that the retry doesn't find an empty transaction
            int b = x;

            return getThreadLocalTransaction().getConfig().allowWriteSkewProblem();
        }

        @TransactionalMethod(allowWriteSkewProblem = false)
        public boolean updateWithDisallowedWriteSkewProblem() {
            //force the loadForRead so that the retry doesn't find an empty transaction
            int b = x;

            return getThreadLocalTransaction().getConfig().allowWriteSkewProblem();
        }

        @TransactionalMethod
        public boolean updateDefaultMethod() {
            //force the loadForRead so that the retry doesn't find an empty transaction
            int b = x;

            Transaction tx = getThreadLocalTransaction();

            return getThreadLocalTransaction().getConfig().allowWriteSkewProblem();
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
            assertFalse(tx.getConfig().isReadonly());
        }

        @TransactionalMethod(readonly = true)
        public void explicitReadonlyMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().isReadonly());
        }

        @TransactionalMethod(readonly = false)
        public void explicitUpdateMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().isReadonly());
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
            assertFalse(tx.getConfig().isInterruptible());
        }

        @TransactionalMethod
        public void defaultMethodWithEmptyTransactionalMethodIsNotInterruptible() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().isInterruptible());
        }

        @TransactionalMethod(interruptible = true)
        public void withInterruptibleSet() throws InterruptedException {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().isInterruptible());
        }

        @TransactionalMethod
        public void withEmptyTransactionalMethodException() throws InterruptedException {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().isInterruptible());
        }

        public void withNoTransactionalMethodButThrowingInterruptibleException() throws InterruptedException {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().isInterruptible());
        }
    }

    @Test
    public void automaticReadTracking() {
        AutomaticReadTracking m = new AutomaticReadTracking();
        m.defaultMethod();
        m.readonlyMethod();
        m.readonlyMethodWithReadTrackingDisabled();
        m.readonlyMethodWithReadTrackingEnabled();
        m.defaultUpdateMethod();
        m.updateMethodWithReadTrackingDisabled();
        m.updateMethodWithReadTrackingEnabled();
    }

    @TransactionalObject
    public static class AutomaticReadTracking {

        private int value;

        public void defaultMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().automaticReadTracking());
        }

        @TransactionalMethod(readonly = true)
        public void readonlyMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().automaticReadTracking());
        }

        @TransactionalMethod(readonly = true, automaticReadTracking = false)
        public void readonlyMethodWithReadTrackingDisabled() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().automaticReadTracking());
        }

        @TransactionalMethod(readonly = true, automaticReadTracking = true)
        public void readonlyMethodWithReadTrackingEnabled() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().automaticReadTracking());
        }

        @TransactionalMethod(readonly = false)
        public void defaultUpdateMethod() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().automaticReadTracking());
        }

        @TransactionalMethod(readonly = false, automaticReadTracking = false)
        public void updateMethodWithReadTrackingDisabled() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().automaticReadTracking());
        }

        @TransactionalMethod(readonly = false, automaticReadTracking = true)
        public void updateMethodWithReadTrackingEnabled() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx.getConfig().automaticReadTracking());
        }
    }
}




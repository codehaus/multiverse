package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.AlphaStm;

import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;

/**
 * Checks if AtomicMethods with different access modifiers are transformed correctly
 */
public class TransactionalMethod_AccessModifierTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        resetInstrumentationProblemMonitor();
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        resetInstrumentationProblemMonitor();
    }

    public static void assertTransactionWorking() {
        assertIsActive(getThreadLocalTransaction());
    }

    @Test
    public void privateMethod() {
        PrivateMethod method = new PrivateMethod();
        method.doIt();
    }

    public static class PrivateMethod {

        @TransactionalMethod
        private void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void publicMethod() {
        PublicMethod method = new PublicMethod();
        method.doIt();
    }

    public static class PublicMethod {

        @TransactionalMethod
        public void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void protectedMethod() {
        ProtectedMethod method = new ProtectedMethod();
        method.doIt();
    }

    public static class ProtectedMethod {

        @TransactionalMethod
        protected void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void packageFriendlyMethod() {
        PackageFriendlyMethod method = new PackageFriendlyMethod();
        method.doIt();
    }

    public static class PackageFriendlyMethod {

        @TransactionalMethod
        void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    @Ignore
    public void abstractMethodFails() {

    }

    @Test
    public void synchronizedMethodGivesNoProblems() {
        SynchronizedMethod method = new SynchronizedMethod();
        method.doIt();
    }

    public class SynchronizedMethod {

        @TransactionalMethod
        public synchronized void doIt() {
            assertTransactionWorking();
        }
    }
}


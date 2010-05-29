package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.javaagent.JavaAgentProblemMonitor;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;

public class TransactionalMethod_InterruptibleTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        resetInstrumentationProblemMonitor();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        resetInstrumentationProblemMonitor();
    }

    @Test
    public void whenNoException_thenError() {
        System.out.println("---------------- following error is expected ----------------------");

        MethodWithoutException o = new MethodWithoutException();

        assertTrue(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }

    @TransactionalObject
    public static class MethodWithoutException {

        private int x;

        @TransactionalMethod(interruptible = true)
        public void method() {
            x = 10;
        }
    }

    @Test
    public void whenNoMatchingException() {
        System.out.println("---------------- following error is expected ----------------------");

        MethodWithIncorrectException o = new MethodWithIncorrectException();
        assertTrue(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }

    @TransactionalObject
    public static class MethodWithIncorrectException {

        private int x;

        @TransactionalMethod(interruptible = true)
        public void method() throws RuntimeException {
            x = 10;
        }
    }

    @Test
    public void whenInterruptedExceptionPresent() {
        MethodWithInterruptedException o = new MethodWithInterruptedException();

        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }


    @TransactionalObject
    public static class MethodWithInterruptedException {

        private int x;

        @TransactionalMethod(interruptible = true)
        public void method() throws InterruptedException {
            x = 10;
        }
    }

    @Test
    public void whenExceptionPresent() {
        MethodWithException o = new MethodWithException();

        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }

    @TransactionalObject
    public static class MethodWithException {

        private int x;

        @TransactionalMethod(interruptible = true)
        public void method() throws Exception {
            x = 10;
        }
    }

    @Test
    public void whenThrowablePresent() {
        MethodWithThrowable o = new MethodWithThrowable();

        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }

    @TransactionalObject
    public static class MethodWithThrowable {

        private int x;

        @TransactionalMethod(interruptible = true)
        public void method() throws Exception {
            x = 10;
        }
    }

    @Test
    public void whenMultipleExceptionsAndAtLeastOneMatching() {
        resetInstrumentationProblemMonitor();

        MethodWithMultipleExceptions o = new MethodWithMultipleExceptions();

        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
    }

    @TransactionalObject
    public static class MethodWithMultipleExceptions {

        private int x;

        @TransactionalMethod(interruptible = true)
        public void method() throws NullPointerException, InterruptedException, IllegalArgumentException {
            x = 10;
        }
    }
}

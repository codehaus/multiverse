package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.instrumentation.InstrumentationProblemMonitor;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;

public class TransactionalMethod_InterruptibleTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        resetInstrumentationProblemMonitor();
    }

    @After
    public void tearDown() {
        resetInstrumentationProblemMonitor();
    }

    @Test
    public void whenNoException_thenError() {
        MethodWithoutException o = new MethodWithoutException();

        assertTrue(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
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
        resetInstrumentationProblemMonitor();

        MethodWithIncorrectException o = new MethodWithIncorrectException();

        assertTrue(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
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
        resetInstrumentationProblemMonitor();

        MethodWithInterruptedException o = new MethodWithInterruptedException();

        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
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
        resetInstrumentationProblemMonitor();

        MethodWithException o = new MethodWithException();

        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
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
        resetInstrumentationProblemMonitor();

        MethodWithThrowable o = new MethodWithThrowable();

        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
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

        assertFalse(InstrumentationProblemMonitor.INSTANCE.isProblemFound());
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

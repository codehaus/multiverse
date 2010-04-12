package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Ignore;
import org.junit.Test;

public class TransactionalMethod_InterruptibleTest {

    @Test
    @Ignore
    public void test() {
    }

//
//    private AlphaStm stm;
//
//    @Before
//    public void setUp() {
//        stm = (AlphaStm) getGlobalStmInstance();
//        resetInstrumentationProblemMonitor();
//    }
//
//    @After
//    public void tearDown() {
//        resetInstrumentationProblemMonitor();
//    }
//
//    @Test
//    public void whenNoException_thenError() {
//        MethodWithoutException o = new MethodWithoutException();
//
//        assertTrue(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
//    }
//
//    @TransactionalObject
//    public static class MethodWithoutException {
//
//        private int x;
//
//        @TransactionalMethod(interruptible = true)
//        public void method() {
//            x = 10;
//        }
//    }
//
//    @Test
//    public void whenNoMatchingException() {
//        resetInstrumentationProblemMonitor();
//
//        MethodWithIncorrectException o = new MethodWithIncorrectException();
//
//        assertTrue(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
//    }
//
//    @TransactionalObject
//    public static class MethodWithIncorrectException {
//
//        private int x;
//
//        @TransactionalMethod(interruptible = true)
//        public void method() throws RuntimeException {
//            x = 10;
//        }
//    }
//
//    @Test
//    public void whenInterruptedExceptionPresent() {
//        resetInstrumentationProblemMonitor();
//
//        MethodWithInterruptedException o = new MethodWithInterruptedException();
//
//        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
//    }
//
//
//    @TransactionalObject
//    public static class MethodWithInterruptedException {
//
//        private int x;
//
//        @TransactionalMethod(interruptible = true)
//        public void method() throws InterruptedException {
//            x = 10;
//        }
//    }
//
//    @Test
//    public void whenExceptionPresent() {
//        resetInstrumentationProblemMonitor();
//
//        MethodWithException o = new MethodWithException();
//
//        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
//    }
//
//    @TransactionalObject
//    public static class MethodWithException {
//
//        private int x;
//
//        @TransactionalMethod(interruptible = true)
//        public void method() throws Exception {
//            x = 10;
//        }
//    }
//
//    @Test
//    public void whenThrowablePresent() {
//        resetInstrumentationProblemMonitor();
//
//        MethodWithThrowable o = new MethodWithThrowable();
//
//        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
//    }
//
//    @TransactionalObject
//    public static class MethodWithThrowable {
//
//        private int x;
//
//        @TransactionalMethod(interruptible = true)
//        public void method() throws Exception {
//            x = 10;
//        }
//    }
//
//    @Test
//    public void whenMultipleExceptionsAndAtLeastOneMatching() {
//        resetInstrumentationProblemMonitor();
//
//        MethodWithMultipleExceptions o = new MethodWithMultipleExceptions();
//
//        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
//    }
//
//    @TransactionalObject
//    public static class MethodWithMultipleExceptions {
//
//        private int x;
//
//        @TransactionalMethod(interruptible = true)
//        public void method() throws NullPointerException, InterruptedException, IllegalArgumentException {
//            x = 10;
//        }
//    }
}

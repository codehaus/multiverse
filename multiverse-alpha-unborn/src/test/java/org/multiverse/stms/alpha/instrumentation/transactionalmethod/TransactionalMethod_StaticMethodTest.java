package org.multiverse.stms.alpha.instrumentation.transactionalmethod;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.TraceLevel;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.refs.IntRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.multiverse.TestUtils.assertIsAlive;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalMethod_StaticMethodTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    public static void assertTransactionWorking() {
        assertIsAlive(getThreadLocalTransaction());
    }

    @Test
    public void testSimpleStaticMethod() {
        long version = stm.getVersion();

        StaticNoArgMethod.doIt();

        assertEquals(version, stm.getVersion());
    }

    public static class StaticNoArgMethod {

        @TransactionalMethod
        static void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void testComplexStaticMethod() {
        StaticComplexMethod.aExpected = 10;
        StaticComplexMethod.bExpected = 1000L;
        StaticComplexMethod.cExpected = "";
        StaticComplexMethod.result = 400;

        int result = StaticComplexMethod.doIt(
                StaticComplexMethod.aExpected,
                StaticComplexMethod.bExpected,
                StaticComplexMethod.cExpected);
        assertEquals(StaticComplexMethod.result, result);
    }

    public static class StaticComplexMethod {

        static int aExpected;
        static long bExpected;
        static String cExpected;
        static int result;

        @TransactionalMethod
        static int doIt(int a, long b, String c) {
            assertTransactionWorking();
            assertEquals(aExpected, a);
            assertEquals(bExpected, b);
            assertSame(cExpected, c);
            return result;
        }
    }

    @Test
    public void atomicObjectsArePassedToStaticMethod() {
        IntRef a = new IntRef(10);
        IntRef b = new IntRef(20);

        long version = stm.getVersion();
        swap(a, b);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(20, a.get());
        assertEquals(10, b.get());
    }

    @TransactionalMethod(traceLevel = TraceLevel.course)
    public static void swap(IntRef a, IntRef b) {
        int oldA = a.get();
        a.set(b.get());
        b.set(oldA);
    }
}

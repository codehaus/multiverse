package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.*;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalMethod_ReturnTypesTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @TransactionalObject
    static class IntValue {
        private int value;

        IntValue(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static void assertTransactionWorking() {
        assertIsActive(getThreadLocalTransaction());
    }

    @Test
    public void voidReturningMethod() {
        VoidReturningMethod m = new VoidReturningMethod();
        m.doIt();
    }

    public class VoidReturningMethod {

        @TransactionalMethod
        public void doIt() {
            assertTransactionWorking();
        }
    }

    @Test
    public void objectReturningMethod() {
        String s = new ObjectReturningMethod().doIt();
        assertEquals("foo", s);
    }

    public class ObjectReturningMethod {

        @TransactionalMethod
        public String doIt() {
            assertTransactionWorking();
            return "foo";
        }
    }

    @Test
    public void integerReturningMethod() {
        Integer result = new IntegerReturningMethod().doIt();
        assertEquals(new Integer(10), result);
    }

    public class IntegerReturningMethod {

        @TransactionalMethod
        public Integer doIt() {
            assertTransactionWorking();
            return new Integer(10);
        }
    }

    // =================== primitives =========================

    @Test
    public void booleanReturningMethod() {
        BooleanReturningMethod m = new BooleanReturningMethod();
        boolean result = m.doIt();
        assertTrue(result);
    }

    public static class BooleanReturningMethod {
        @TransactionalMethod
        public boolean doIt() {
            assertTransactionWorking();
            return true;
        }
    }

    @Test
    public void byteReturningMethod() {
        ByteReturningMethod m = new ByteReturningMethod();
        byte result = m.doIt();
        assertEquals(11, result);
    }

    public static class ByteReturningMethod {
        @TransactionalMethod
        public byte doIt() {
            assertTransactionWorking();
            return 11;
        }
    }

    @Test
    public void shortReturningMethod() {
        ShortReturningMethod m = new ShortReturningMethod();
        short result = m.doIt();
        assertEquals(11, result);
    }

    public static class ShortReturningMethod {
        @TransactionalMethod
        public short doIt() {
            assertTransactionWorking();
            return 11;
        }
    }

    @Test
    public void charReturningMethod() {
        CharReturningMethod m = new CharReturningMethod();
        char result = m.doIt();
        assertEquals('p', result);
    }

    public static class CharReturningMethod {
        @TransactionalMethod
        public char doIt() {
            assertTransactionWorking();
            return 'p';
        }
    }

    @Test
    public void intReturningMethod() {
        IntReturningMethod m = new IntReturningMethod();
        int result = m.doIt();
        assertEquals(111, result);
    }

    public static class IntReturningMethod {
        @TransactionalMethod
        public int doIt() {
            assertTransactionWorking();
            return 111;
        }
    }

    @Test
    public void floatReturningMehod() {
        FloatReturningMethod m = new FloatReturningMethod();
        float result = m.doIt();
        assertEquals(11.4f, result, 0.0001);
    }

    public static class FloatReturningMethod {
        @TransactionalMethod
        public float doIt() {
            assertTransactionWorking();
            return 11.4f;
        }
    }

    @Test
    public void longReturningMethod() {
        LongReturningMethod m = new LongReturningMethod();
        long result = m.doIt();
        assertEquals(111, result);
    }

    public static class LongReturningMethod {
        @TransactionalMethod
        public long doIt() {
            assertTransactionWorking();
            return 111L;
        }
    }

    @Test
    public void doubleReturningMethod() {
        DoubleReturningMethod m = new DoubleReturningMethod();
        double result = m.doIt();
        assertEquals(11.4, result, 0.0001);
    }

    public static class DoubleReturningMethod {
        @TransactionalMethod
        public double doIt() {
            assertTransactionWorking();
            return 11.4;
        }
    }

    // ================== arrays ======================

    @Test
    public void primitiveArrayReturningMethod() {
        PrimitiveArrayReturningMethod m = new PrimitiveArrayReturningMethod();
        boolean[] result = m.doIt();
        assertEquals(2, result.length);
        assertTrue(result[0]);
        assertFalse(result[1]);
    }

    public static class PrimitiveArrayReturningMethod {
        @TransactionalMethod
        public boolean[] doIt() {
            assertTransactionWorking();
            return new boolean[]{true, false};
        }
    }

    @Test
    public void objectArrayReturningMethod() {
        String[] array = new String[]{"foo", "bar"};
        ObjectArrayReturningMethod m = new ObjectArrayReturningMethod();
        m.result = array;
        Object[] result = m.doIt();
        assertSame(array, result);
    }

    public static class ObjectArrayReturningMethod {
        Object[] result;

        @TransactionalMethod
        public Object[] doIt() {
            assertTransactionWorking();
            return result;
        }
    }
}

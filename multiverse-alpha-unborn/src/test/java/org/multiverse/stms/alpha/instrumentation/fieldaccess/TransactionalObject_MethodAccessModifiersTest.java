package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Stm;
import org.multiverse.stms.alpha.AlphaStm;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_MethodAccessModifiersTest {

    private Stm stm;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void testTxObjectCallingProtectedMethod() {
        TxObjectCallingProtectedMethod o = new TxObjectCallingProtectedMethod();

        long version = stm.getVersion();
        o.callDoIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingProtectedMethod {

        private int value;

        public TxObjectCallingProtectedMethod() {
            value = 0;
        }

        protected void doIt() {
            value++;
        }

        public int getValue() {
            return value;
        }

        public void callDoIt() {
            doIt();
        }
    }

    @Test
    public void testTxObjectCallingPublicMethod() {
        TxObjectCallingPublicMethod o = new TxObjectCallingPublicMethod();

        long version = stm.getVersion();
        o.callDoIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingPublicMethod {

        private int value;

        public TxObjectCallingPublicMethod() {
            value = 0;
        }


        public void doIt() {
            value++;
        }

        public int getValue() {
            return value;
        }

        public void callDoIt() {
            doIt();
        }
    }

    @Test
    public void testTxObjectCallingPackageFriendlyMethod() {
        TxObjectCallingPackageFriendlyMethod o = new TxObjectCallingPackageFriendlyMethod();

        long version = stm.getVersion();
        o.callDoIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingPackageFriendlyMethod {

        private int value;

        public TxObjectCallingPackageFriendlyMethod() {
            value = 0;
        }


        void doIt() {
            value++;
        }

        public int getValue() {
            return value;
        }

        public void callDoIt() {
            doIt();
        }
    }

    @Test
    public void testTxObjectCallingPrivateMethod() {
        TxObjectCallingPrivateMethod privateMethod = new TxObjectCallingPrivateMethod();

        long version = stm.getVersion();
        privateMethod.callDoIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, privateMethod.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingPrivateMethod {

        private int value;

        public TxObjectCallingPrivateMethod() {
            value = 0;
        }

        private void doIt() {
            value++;
        }

        public int getValue() {
            return value;
        }

        public void callDoIt() {
            doIt();
        }
    }

    @Test
    public void testStaticPublicMethod() {
        TxObjectCallingPublicStaticMethod o = new TxObjectCallingPublicStaticMethod();

        long version = stm.getVersion();
        o.doIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingPublicStaticMethod {

        public static int inc(int number) {
            return ++number;
        }

        private int value;

        public TxObjectCallingPublicStaticMethod() {
            value = 0;
        }

        public void doIt() {
            value = inc(value);
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testStaticProtectedMethod() {
        TxObjectCallingProtectedStaticMethod o = new TxObjectCallingProtectedStaticMethod();

        long version = stm.getVersion();
        o.doIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingProtectedStaticMethod {

        protected static int inc(int number) {
            return ++number;
        }

        private int value;

        public TxObjectCallingProtectedStaticMethod() {
            this.value = 0;
        }

        public void doIt() {
            value = inc(value);
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testStaticPackageFriendlyMethod() {
        TxObjectCallingPackageFriendlyStaticMethod o = new TxObjectCallingPackageFriendlyStaticMethod();

        long version = stm.getVersion();
        o.doIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingPackageFriendlyStaticMethod {

        static int inc(int number) {
            return ++number;
        }

        private int value;

        public TxObjectCallingPackageFriendlyStaticMethod() {
            value = 0;
        }

        void doIt() {
            value = inc(value);
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testStaticPrivateMethod() {
        TxObjectCallingPrivateStaticMethod o = new TxObjectCallingPrivateStaticMethod();

        long version = stm.getVersion();
        o.doIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingPrivateStaticMethod {

        private static int inc(int number) {
            return ++number;
        }

        public TxObjectCallingPrivateStaticMethod() {
            value = 0;
        }


        private int value;

        public void doIt() {
            value = inc(value);
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testExternalStaticMethod() {
        TxObjectCallingExternalStaticMethod o = new TxObjectCallingExternalStaticMethod();

        long version = stm.getVersion();
        o.doIt();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(20, o.getValue());
    }

    @TransactionalObject
    static class TxObjectCallingExternalStaticMethod {

        private int value;

        public void doIt() {
            value = Math.max(10, 20);
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testSynchronizedMethod() throws NoSuchMethodException {
        Method method = SynchronizedMethod.class.getMethod("doIt");
        assertTrue(Modifier.isSynchronized(method.getModifiers()));

        SynchronizedMethod m = new SynchronizedMethod();
        long version = stm.getVersion();
        m.doIt();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(10, m.getValue());
    }

    @TransactionalObject
    static class SynchronizedMethod {

        private int value;

        public synchronized void doIt() {
            value = 10;
        }

        public int getValue() {
            return value;
        }
    }

}

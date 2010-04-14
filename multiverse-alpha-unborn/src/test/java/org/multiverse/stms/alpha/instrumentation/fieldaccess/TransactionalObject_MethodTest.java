package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_MethodTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    // ================== return values ===========================

    @Test
    public void txObjectAsReturnValue() {
        TxObjectAsReturnValue child = new TxObjectAsReturnValue();
        TxObjectAsReturnValue parent = new TxObjectAsReturnValue();

        long version = stm.getVersion();

        child.setRef(parent);

        assertEquals(version + 1, stm.getVersion());
        assertSame(parent, child.getRef());
        assertNull(parent.getRef());
    }

    @TransactionalObject
    static class TxObjectAsReturnValue {

        private TxObjectAsReturnValue ref;

        public TxObjectAsReturnValue() {
            ref = null;
        }

        public TxObjectAsReturnValue(TxObjectAsReturnValue ref) {
            this.ref = ref;
        }

        public TxObjectAsReturnValue getRef() {
            return ref;
        }

        public void setRef(TxObjectAsReturnValue ref) {
            this.ref = ref;
        }
    }

    @Test
    public void testRuntimeKnownReturnTypeWithNonTxObject() {
        Object item = "foo";
        ObjectReturn objectReturn = new ObjectReturn(item);
        assertSame(item, objectReturn.doIt());
    }

    @Test
    public void testRuntimeKnownReturnTypeWithTxObject() {
        Object item = new TransactionalInteger();
        ObjectReturn objectReturn = new ObjectReturn(item);
        assertSame(item, objectReturn.doIt());
    }


    @TransactionalObject
    private static class ObjectReturn {

        private Object value;

        private ObjectReturn(Object value) {
            this.value = value;
        }

        public Object theMethod() {
            return value;
        }

        public Object doIt() {
            return theMethod();
        }
    }


    @Test
    public void testNonTxObjectReturnValue() {
        String value = "foo";
        StringReturn r = new StringReturn(value);
        assertSame(value, r.doIt());
    }

    @Test
    public void testNonTxObjectReturnValueThatIsNull() {
        StringReturn r = new StringReturn(null);
        assertNull(r.doIt());
    }

    @TransactionalObject
    private static class StringReturn {

        private String value;

        private StringReturn(String value) {
            this.value = value;
        }

        public String theMethod() {
            return value;
        }

        public String doIt() {
            return theMethod();
        }
    }


    @Test
    public void booleanReturnValue() {
        BooleanReturn b = new BooleanReturn(true);
        assertTrue(b.doIt());
    }

    @TransactionalObject
    private static class BooleanReturn {

        private boolean value;

        private BooleanReturn(boolean value) {
            this.value = value;
        }

        public boolean theMethod() {
            return value;
        }

        public boolean doIt() {
            return theMethod();
        }
    }

    @Test
    public void shortReturnValue() {
        shortReturn shortReturn = new shortReturn((short) 100);
        assertEquals(100, shortReturn.doIt());
    }

    @TransactionalObject
    private static class shortReturn {

        private short value;

        private shortReturn(short value) {
            this.value = value;
        }

        public short theMethod() {
            return value;
        }

        public short doIt() {
            return theMethod();
        }
    }

    @Test
    public void byteReturnValue() {
        byteReturn byteReturn = new byteReturn((byte) 100);
        assertEquals(100, byteReturn.doIt());
    }

    @TransactionalObject
    private static class byteReturn {

        private byte value;

        private byteReturn(byte value) {
            this.value = value;
        }

        public byte theMethod() {
            return value;
        }

        public byte doIt() {
            return theMethod();
        }
    }

    @Test
    public void charReturnValue() {
        charReturn charReturn = new charReturn((char) 100);
        assertEquals(100, charReturn.doIt());
    }

    @TransactionalObject
    private static class charReturn {

        private char value;

        private charReturn(char value) {
            this.value = value;
        }

        public char theMethod() {
            return value;
        }

        public char doIt() {
            return theMethod();
        }
    }


    @Test
    public void intReturnValue() {
        intReturn intReturn = new intReturn(100);
        assertEquals(100, intReturn.doIt());

    }

    @TransactionalObject
    private static class intReturn {

        private int value;

        private intReturn(int value) {
            this.value = value;
        }

        public int theMethod() {
            return value;
        }

        public int doIt() {
            return theMethod();
        }
    }

    @Test
    public void floatReturnValue() {
        floatReturn floatReturn = new floatReturn(100);
        assertEquals(100, floatReturn.doIt(), 0.00001);
    }

    @TransactionalObject
    private static class floatReturn {

        private float value;

        private floatReturn(float value) {
            this.value = value;
        }

        public float theMethod() {
            return value;
        }

        public float doIt() {
            return theMethod();
        }
    }

    @Test
    public void longReturnValue() {
        longReturn longReturn = new longReturn(100);
        assertEquals(100, longReturn.doIt());

    }

    @TransactionalObject
    private static class longReturn {

        private long value;

        private longReturn(long value) {
            this.value = value;
        }

        public long theMethod() {
            return value;
        }

        public long doIt() {
            return theMethod();
        }
    }

    @Test
    public void doubleReturnValue() {
        doubleReturn doubleReturn = new doubleReturn(100);
        assertEquals(100, doubleReturn.doIt(), 0.00001);
    }

    @TransactionalObject
    private static class doubleReturn {

        private double value;

        private doubleReturn(double value) {
            this.value = value;
        }

        public double theMethod() {
            return value;
        }

        public double doIt() {
            return theMethod();
        }
    }


    // ====================== all kinds of argument types ===================

    @Test
    public void booleanArg() {
        booleanArg booleanArg = new booleanArg();
        booleanArg.doIt(true);
        assertTrue(booleanArg.getArg());
        booleanArg.doIt(false);
        assertFalse(booleanArg.getArg());
    }


    @TransactionalObject
    public static class booleanArg {

        boolean arg;

        public void doIt(boolean arg) {
            this.arg = arg;
        }

        public boolean getArg() {
            return arg;
        }
    }

    @Test
    public void charArg() {
        charArg charArg = new charArg();
        charArg.doIt((char) 20);
        assertEquals(20, charArg.getArg());
    }

    @TransactionalObject
    public static class charArg {

        char arg;

        public void doIt(char arg) {
            this.arg = arg;
        }

        public char getArg() {
            return arg;
        }
    }

    @Test
    public void byteArg() {
        byteArg byteArg = new byteArg();
        byteArg.doIt((byte) 20);
        assertEquals(20, byteArg.getArg());
    }

    @TransactionalObject
    public static class byteArg {

        byte arg;

        public void doIt(byte arg) {
            this.arg = arg;
        }

        public byte getArg() {
            return arg;
        }
    }


    @Test
    public void shortArg() {
        shortArg shortArg = new shortArg();
        shortArg.doIt((short) 20);
        assertEquals(20, shortArg.getArg());
    }

    @TransactionalObject
    public static class shortArg {

        short arg;

        public void doIt(short arg) {
            this.arg = arg;
        }

        public short getArg() {
            return arg;
        }
    }


    @Test
    public void intArg() {
        intArg intArg = new intArg();
        intArg.doIt(20);
        assertEquals(20, intArg.getArg());
    }

    @TransactionalObject
    static class intArg {

        int arg;

        public void doIt(int arg) {
            this.arg = arg;
        }

        public int getArg() {
            return arg;
        }
    }

    @Test
    public void floatArg() {
        floatArg floatArg = new floatArg();
        floatArg.doIt(20);
        assertEquals(20, floatArg.getArg(), 0.000001);
    }

    @TransactionalObject
    public static class floatArg {

        float arg;

        public void doIt(float arg) {
            this.arg = arg;
        }

        public float getArg() {
            return arg;
        }
    }

    @Test
    public void longArg() {
        longArg longArg = new longArg();
        longArg.doIt(20);
        assertEquals(20, longArg.getArg());
    }

    @TransactionalObject
    public static class longArg {

        long arg;

        public void doIt(long arg) {
            this.arg = arg;
        }

        public long getArg() {
            return arg;
        }
    }

    @Test
    public void doubleArg() {
        doubleArg doubleArg = new doubleArg();
        doubleArg.doIt(20);
        assertEquals(20, doubleArg.getArg(), 0.0001);
    }

    @TransactionalObject
    public static class doubleArg {

        double arg;

        public void doIt(double arg) {
            this.arg = arg;
        }

        public double getArg() {
            return arg;
        }
    }

    @Test
    public void arrayArg() {
        int[] array = new int[]{1, 2, 3, 4};
        arrayArg arrayArg = new arrayArg();
        arrayArg.doIt(array);
        assertSame(array, arrayArg.getArg());
        arrayArg.doIt(null);
        assertNull(arrayArg.getArg());
    }

    @TransactionalObject
    public static class arrayArg {

        int[] arg;

        public void doIt(int[] arg) {
            this.arg = arg;
        }

        public int[] getArg() {
            return arg;
        }
    }

    @Test
    public void varArgs() {
        int[] array = new int[]{1, 2, 3, 4};
        varArg arrayArg = new varArg();
        arrayArg.doIt(array);
        assertSame(array, arrayArg.getArg());
        arrayArg.doIt(null);
        assertNull(arrayArg.getArg());
    }

    @TransactionalObject
    public static class varArg {

        int[] arg;

        public void doIt(int... arg) {
            this.arg = arg;
        }

        public int[] getArg() {
            return arg;
        }
    }

    @Test
    public void nonTxObjectArg() {
        String arg = "foo";
        NonTxObjectArg nonTxObjectArg = new NonTxObjectArg();
        nonTxObjectArg.doIt(arg);
        assertSame(arg, nonTxObjectArg.getArg());
        nonTxObjectArg.doIt(null);
        assertNull(nonTxObjectArg.getArg());
    }

    @TransactionalObject
    public static class NonTxObjectArg {

        private String arg;

        public void doIt(String arg) {
            this.arg = arg;
        }

        public String getArg() {
            return arg;
        }
    }

    @Test
    public void txObjectAsArgument() {

        TxObjectArg o = new TxObjectArg();
        o.inc();
        Assert.assertEquals(1, o.getRef().get());
    }

    @TransactionalObject
    static class TxObjectArg {

        private TransactionalInteger ref;

        public TxObjectArg() {
            this.ref = new TransactionalInteger(0);
        }

        public void inc() {
            doIt(ref);
        }

        public void doIt(TransactionalInteger intRef) {
            intRef.inc();
        }

        public TransactionalInteger getRef() {
            return ref;
        }
    }

    @Test
    public void runtimeKnownArg() {
        RuntimeKnownArg runtimeKnownArg = new RuntimeKnownArg();

        NonTxObjectRunnable firstValue = new NonTxObjectRunnable();
        runtimeKnownArg.doIt(firstValue);
        assertSame(firstValue, runtimeKnownArg.getArg());
        assertEquals(1, firstValue.getValue());

        TxObjectRunnable secondValue = new TxObjectRunnable();
        runtimeKnownArg.doIt(secondValue);
        assertSame(secondValue, runtimeKnownArg.getArg());
        assertEquals(1, secondValue.getValue());
    }

    @TransactionalObject
    static class TxObjectRunnable implements Runnable {

        int value;

        public TxObjectRunnable() {
            value = 0;
        }

        public void run() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    class NonTxObjectRunnable implements Runnable {

        int value;

        public void run() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    @TransactionalObject
    static class RuntimeKnownArg {

        Runnable arg;

        public void doIt(Runnable arg) {
            this.arg = arg;
            arg.run();
        }

        public Runnable getArg() {
            return arg;
        }
    }

    // ============================

    @Test
    public void txObjectIsCreatedInMethod() {
        TxObjectIsCreatedInMethod o = new TxObjectIsCreatedInMethod();

        long version = stm.getVersion();
        o.doIt();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(20, o.getRef().get());
    }

    @TransactionalObject
    static class TxObjectIsCreatedInMethod {

        private TransactionalInteger ref;

        public void doIt() {
            ref = new TransactionalInteger(20);
        }

        public TransactionalInteger getRef() {
            return ref;
        }
    }


    @Test
    public void txObjectAsArgumentProblem() {
        TxObjectArgProblem o1 = new TxObjectArgProblem(1);
        TxObjectArgProblem o2 = new TxObjectArgProblem(2);

        o1.method1(o2);

        assertEquals(1, o1.getSomefield());
        assertEquals(1, o2.getSomefield());
    }

    @TransactionalObject
    static class TxObjectArgProblem {

        protected int somefield;

        TxObjectArgProblem(int somefield) {
            this.somefield = somefield;
        }

        public int getSomefield() {
            return somefield;
        }

        public void method2(TxObjectArgProblem ref) {
            somefield = ref.somefield;
        }

        public void method1(TxObjectArgProblem ref) {
            ref.method2(this);
        }
    }
}

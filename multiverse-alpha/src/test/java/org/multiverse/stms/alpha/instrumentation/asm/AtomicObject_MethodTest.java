package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class AtomicObject_MethodTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    // ================== return values ===========================

    @Test
    public void atomicObjectAsReturnValue() {
        AtomicObjectAsReturnValue child = new AtomicObjectAsReturnValue();
        AtomicObjectAsReturnValue parent = new AtomicObjectAsReturnValue();

        long version = stm.getTime();

        child.setRef(parent);

        assertEquals(version + 1, stm.getTime());
        assertSame(parent, child.getRef());
        assertNull(parent.getRef());
    }

    @AtomicObject
    static class AtomicObjectAsReturnValue {

        private AtomicObjectAsReturnValue ref;

        public AtomicObjectAsReturnValue() {
        }

        public AtomicObjectAsReturnValue(AtomicObjectAsReturnValue ref) {
            this.ref = ref;
        }

        public AtomicObjectAsReturnValue getRef() {
            return ref;
        }

        public void setRef(AtomicObjectAsReturnValue ref) {
            this.ref = ref;
        }
    }

    @Test
    public void testRuntimeKnownReturnTypeWithNonAtomicObject() {
        Object item = "foo";
        ObjectReturn objectReturn = new ObjectReturn(item);
        assertSame(item, objectReturn.doIt());
    }

    @Test
    public void testRuntimeKnownReturnTypeWithAtomicObject() {
        Object item = new IntRef();
        ObjectReturn objectReturn = new ObjectReturn(item);
        assertSame(item, objectReturn.doIt());
    }


    @AtomicObject
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
    public void testNonAtomicObjectReturnValue() {
        String value = "foo";
        StringReturn r = new StringReturn(value);
        assertSame(value, r.doIt());
    }

    @Test
    public void testNonAtomicObjectReturnValueThatIsNull() {
        StringReturn r = new StringReturn(null);
        assertNull(r.doIt());
    }

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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


    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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

    @AtomicObject
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
    public void nonAtomicObjectArg() {
        String arg = "foo";
        NonAtomicObjectArg nonAtomicObjectArg = new NonAtomicObjectArg();
        nonAtomicObjectArg.doIt(arg);
        assertSame(arg, nonAtomicObjectArg.getArg());
        nonAtomicObjectArg.doIt(null);
        assertNull(nonAtomicObjectArg.getArg());
    }

    @AtomicObject
    public static class NonAtomicObjectArg {

        private String arg;

        public void doIt(String arg) {
            this.arg = arg;
        }

        public String getArg() {
            return arg;
        }
    }

    @Test
    public void atomicObjectAsArgument() {

        AtomicObjectArg o = new AtomicObjectArg();
        o.inc();
        Assert.assertEquals(1, o.getIntRef().get());
    }

    @AtomicObject
    static class AtomicObjectArg {

        private IntRef intRef;

        public AtomicObjectArg() {
            this.intRef = new IntRef(0);
        }

        public void inc() {
            doIt(intRef);
        }

        public void doIt(IntRef intRef) {
            intRef.inc();
        }

        public IntRef getIntRef() {
            return intRef;
        }
    }

    @Test
    public void runtimeKnownArg() {
        RuntimeKnownArg runtimeKnownArg = new RuntimeKnownArg();

        NonAtomicObjectRunnable firstValue = new NonAtomicObjectRunnable();
        runtimeKnownArg.doIt(firstValue);
        assertSame(firstValue, runtimeKnownArg.getArg());
        assertEquals(1, firstValue.getValue());

        AtomicObjectRunnable secondValue = new AtomicObjectRunnable();
        runtimeKnownArg.doIt(secondValue);
        assertSame(secondValue, runtimeKnownArg.getArg());
        assertEquals(1, secondValue.getValue());
    }

    @AtomicObject
    static class AtomicObjectRunnable implements Runnable {

        int value;

        public void run() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    class NonAtomicObjectRunnable implements Runnable {

        int value;

        public void run() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    @AtomicObject
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
    public void atomicObjectIsCreatedInMethod() {
        AtomicObjectIsCreatedInMethod o = new AtomicObjectIsCreatedInMethod();

        long version = stm.getTime();
        o.doIt();
        assertEquals(version + 1, stm.getTime());
        assertEquals(20, o.getRef().get());
    }

    @AtomicObject
    static class AtomicObjectIsCreatedInMethod {

        private IntRef ref;

        public void doIt() {
            ref = new IntRef(20);
        }

        public IntRef getRef() {
            return ref;
        }
    }


    @Test
    public void atomicObjectAsArgumentProblem() {
        AtomicObjectArgProblem o1 = new AtomicObjectArgProblem(1);
        AtomicObjectArgProblem o2 = new AtomicObjectArgProblem(2);

        o1.method1(o2);

        assertEquals(1, o1.getSomefield());
        assertEquals(1, o2.getSomefield());
    }

    @AtomicObject
    static class AtomicObjectArgProblem {

        protected int somefield;

        AtomicObjectArgProblem(int somefield) {
            this.somefield = somefield;
        }

        public int getSomefield() {
            return somefield;
        }

        public void method2(AtomicObjectArgProblem ref) {
            somefield = ref.somefield;
        }

        public void method1(AtomicObjectArgProblem ref) {
            ref.method2(this);
        }
    }
}

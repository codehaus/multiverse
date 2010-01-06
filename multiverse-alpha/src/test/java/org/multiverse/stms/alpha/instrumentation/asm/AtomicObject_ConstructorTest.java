package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaStm;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Peter Veentjer
 */
public class AtomicObject_ConstructorTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void testConflictingConstructor() {
        ConflictingConstructor conflictingConstructor = new ConflictingConstructor();
        //todo
    }

    @AtomicObject
    public static class ConflictingConstructor {

        private int field;

        public ConflictingConstructor() {

        }

        public ConflictingConstructor(ConflictingConstructor conflictingConstructor) {

        }
    }

    @Test
    public void testNoArgConstructor() {
        NoArgConstructor o = new NoArgConstructor();
        assertEquals(10, o.getValue());
    }

    @AtomicObject
    static class NoArgConstructor {

        private int value;

        public NoArgConstructor() {
            value = 10;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void testEmptyConstructor() {
        long version = stm.getTime();

        testEmptyConstructor o = new testEmptyConstructor();

        assertEquals(version, stm.getTime());
    }

    @AtomicObject
    static class testEmptyConstructor {

        private int value;

        public testEmptyConstructor() {
        }
    }

    @Test
    public void testNoConstructor() {
        long version = stm.getTime();

        NoConstructor noConstructor = new NoConstructor();

        assertEquals(version, stm.getTime());
    }

    @AtomicObject
    static class NoConstructor {

        private int value;
    }

    @Test
    public void testOneArgConstructor() {
        long version = stm.getTime();

        OneArgConstructor o = new OneArgConstructor(10);

        assertEquals(version + 1, stm.getTime());
        assertEquals(10, o.getA());
    }

    @AtomicObject
    static class OneArgConstructor {

        private int a;

        public OneArgConstructor(int a) {
            this.a = a;
        }

        public int getA() {
            return a;
        }
    }

    @Test
    public void testTwoArgConstructor() {
        long version = stm.getTime();

        TwoArgConstructor o = new TwoArgConstructor(20, 8);

        assertEquals(version + 1, stm.getTime());
        assertEquals(20, o.getA());
        assertEquals(8, o.getB());
    }

    @AtomicObject
    static class TwoArgConstructor {

        private int a;
        private int b;

        public TwoArgConstructor(int a, int b) {
            this.a = a;
            this.b = b;
        }

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }
    }

    @Test
    public void testFiveArgConstructor() {
        long version = stm.getTime();

        FiveArgConstructor fiveArgConstructor = new FiveArgConstructor(10, 40, 2, 9, -1);

        assertEquals(version + 1, stm.getTime());
        assertEquals(10, fiveArgConstructor.getA());
        assertEquals(40, fiveArgConstructor.getB());
        assertEquals(2, fiveArgConstructor.getC());
        assertEquals(9, fiveArgConstructor.getD());
        assertEquals(-1, fiveArgConstructor.getE());
    }

    @AtomicObject
    static class FiveArgConstructor {

        private int a;
        private int b;
        private int c;
        private int d;
        private int e;

        public FiveArgConstructor(int a, int b, int c, int d, int e) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
            this.e = e;
        }

        public int getA() {
            return a;
        }

        public int getB() {
            return b;
        }

        public int getC() {
            return c;
        }

        public int getD() {
            return d;
        }

        public int getE() {
            return e;
        }
    }

    @Test
    public void testVarArgsConstructor() {
        long version = stm.getTime();

        VarArgConstructor varArgConstructor = new VarArgConstructor(1, 2, 3, 4);

        assertEquals(version + 1, stm.getTime());
        assertArrayEquals(new int[]{1, 2, 3, 4}, varArgConstructor.getArgs());
    }

    @AtomicObject
    static class VarArgConstructor {

        private int[] args;

        VarArgConstructor(int... args) {
            this.args = args;
        }

        public int[] getArgs() {
            return args;
        }
    }

    @Test
    public void testArrayConstructor() {
        long version = stm.getTime();

        int[] args = new int[]{1, 2, 3, 4};
        ArrayConstructor o = new ArrayConstructor(args);

        assertEquals(version + 1, stm.getTime());
        assertSame(args, o.getArgs());
    }

    @AtomicObject
    static class ArrayConstructor {

        private int[] args;

        ArrayConstructor(int[] args) {
            this.args = args;
        }

        public int[] getArgs() {
            return args;
        }
    }

    @Test
    public void testTransactionalObjectConstructor() {
        IntRef intValue = new IntRef(10);

        long version = stm.getTime();

        TransactionalObjectConstructor o = new TransactionalObjectConstructor(intValue);

        assertEquals(version + 1, stm.getTime());
        assertSame(intValue, o.getIntValue());
    }

    @AtomicObject
    static class TransactionalObjectConstructor {

        IntRef intValue;

        TransactionalObjectConstructor(IntRef intValue) {
            this.intValue = intValue;
        }

        public IntRef getIntValue() {
            return intValue;
        }
    }

    @Test
    public void testNormalRefConstructor() {
        String value = "foo";

        long version = stm.getTime();

        NormalRefConstructor o = new NormalRefConstructor(value);

        assertEquals(version + 1, stm.getTime());
        assertSame(value, o.getValue());
    }

    @AtomicObject
    static class NormalRefConstructor {

        String value;

        NormalRefConstructor(String intValue) {
            this.value = intValue;
        }

        public String getValue() {
            return value;
        }
    }

    @Test
    public void testCreateAtomicObjectInConstructor() {
        IntRef ref = new IntRef(1);

        long startVersion = stm.getTime();

        MetadataRepository s = MetadataRepository.INSTANCE;

        CreateAtomicObjectInConstructor o = new CreateAtomicObjectInConstructor(10);

        assertEquals(startVersion + 1, stm.getTime());
        assertNotNull(o.getValue());
    }

    @AtomicObject
    static class CreateAtomicObjectInConstructor {

        private IntRef value;


        CreateAtomicObjectInConstructor(int i) {
            this.value = new IntRef(i);
        }

        public IntRef getValue() {
            return value;
        }
    }

    @Test
    public void testExceptionIsThrownInsideConstructor() {
        long version = stm.getTime();

        try {
            new ExceptionIsThrownInsideConstructor();
            fail();
        } catch (MyException ex) {

        }

        assertEquals(version, stm.getTime());
    }

    @AtomicObject
    static class ExceptionIsThrownInsideConstructor {

        private int field;

        ExceptionIsThrownInsideConstructor() {
            throw new MyException();
        }
    }

    static class MyException extends RuntimeException {

    }

    @Test
    public void constructorWithExplicitSuperCallingConstructor() {
        long version = stm.getTime();

        AtomicObjectWithNoArgSuperCallingConstructor o = new AtomicObjectWithNoArgSuperCallingConstructor();

        assertEquals(version, stm.getTime());
    }

    @AtomicObject
    static class AtomicObjectWithNoArgSuperCallingConstructor {

        private int value;

        AtomicObjectWithNoArgSuperCallingConstructor() {
            super();
        }
    }

    @Test
    public void constructorWithSuperCallingConstructor() {
        //todo
    }


    @Test
    public void atomicObjectWithNoArgThisCallingConstructor() {
        //todo
    }

    @Test
    public void constructorThatCallsThis() {
        long version = stm.getTime();
        //ThisCallingConstructor c = new ThisCallingConstructor();
        //assertEquals(25, c.getValue());
        //assertEquals(version + 1, stm.getClockVersion());
        //todo
    }

    @AtomicObject
    static class ThisCallingConstructor {

        int value;

        public ThisCallingConstructor() {
            this(25);
            //    System.out.println("value = "+value);
        }

        public ThisCallingConstructor(int value) {
            //    System.out.println("constructor called with value: "+value);
            this.value = value;
            //    System.out.println("value set: "+this.value);
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    //test inner classes

    // ============================== finals ==========================

    @Test
    public void finalFieldOnAtomicObject() {
        long version = stm.getTime();

        AtomicObjectWithFinalField o = new AtomicObjectWithFinalField(10);
        assertFalse(o instanceof AlphaAtomicObject);
        assertEquals(version, stm.getTime());
        assertEquals(10, o.value);
    }

    @AtomicObject
    static class AtomicObjectWithFinalField {

        final int value;

        AtomicObjectWithFinalField(int value) {
            this.value = value;
        }
    }

    @Test
    public void finalFieldIsAtomicObject() {
        IntRef value = new IntRef(100);

        long version = stm.getTime();

        AtomicObjectWithOtherAtomicObjectAsFinalField o = new AtomicObjectWithOtherAtomicObjectAsFinalField(value);
        assertFalse(o instanceof AlphaAtomicObject);
        assertEquals(version, stm.getTime());
        assertEquals(value, o.value);
    }

    @AtomicObject
    static class AtomicObjectWithOtherAtomicObjectAsFinalField {

        final IntRef value;

        AtomicObjectWithOtherAtomicObjectAsFinalField(IntRef value) {
            this.value = value;
        }
    }

    @Test
    public void finalFieldIsAtomicObjectAndCreatedInConstructor() {
        long version = stm.getTime();

        AtomicObjectWithFinalRefThatIsCreatedInsideConstructor o = new AtomicObjectWithFinalRefThatIsCreatedInsideConstructor();
        assertFalse(o instanceof AlphaAtomicObject);
        assertEquals(10, o.value.get());
        assertEquals(version + 1, stm.getTime());
    }

    @AtomicObject
    static class AtomicObjectWithFinalRefThatIsCreatedInsideConstructor {

        final IntRef value;

        public AtomicObjectWithFinalRefThatIsCreatedInsideConstructor() {
            value = new IntRef(10);
        }
    }

    @Test
    public void testStaticInitializer() {
        //todo
    }

    @Test
    public void testInstanceInitializer() {
        constructorInitCounter.set(0);

        InstanceInitializer foo = new InstanceInitializer();

        //if the executor has executed correct, the counter should be 1.
        assertEquals(1, constructorInitCounter.get());
        //and the value should be the oldvalue+1
        assertEquals(1, foo.getInitCounter());

        //if the constructor code is executed again, the initCounter is increased
        //and this is not what we want. The reset counter should not change anymore
        assertEquals(1, constructorInitCounter.get());
    }

    private static final AtomicLong constructorInitCounter = new AtomicLong();

    @AtomicObject
    public static class InstanceInitializer {

        long initCounter = constructorInitCounter.incrementAndGet();

        public InstanceInitializer() {
        }

        public long getInitCounter() {
            return initCounter;
        }
    }

}

package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Stm;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import java.lang.reflect.Field;

import static java.lang.reflect.Modifier.isTransient;
import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.alpha.instrumentation.AlphaReflectionUtils.*;

/**
 * Test all interesting field configurations for an TransactionalObject.
 *
 * @author Peter Veentjer
 */
public class TransactionalObject_FieldTypesTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void testVolatile() {
        VolatileField o = new VolatileField();
        assertFalse(o instanceof AlphaTransactionalObject);

        assertTrue(existsField(VolatileField.class, "field"));
    }

    @TransactionalObject
    public static class VolatileField {
        volatile int field;
    }

    @Test
    public void testTransient() {
        TransientField o = new TransientField();
        assertTrue(o instanceof AlphaTransactionalObject);

        Field transientField = getTranlocalField(TransientField.class, "field");
        assertTrue(isTransient(transientField.getModifiers()));
        assertFalse(existsField(TransientField.class, "field"));
    }

    @TransactionalObject
    public static class TransientField {
        transient int field;
    }

    @Test
    public void noFieldsTest() {
        NoFields value = new NoFields();
        assertFalse(value instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class NoFields {
    }

    @Test
    public void booleanTest() {
        booleanValue value = new booleanValue(true);
        assertTrue(value.get());

        value.set(false);
        assertFalse(value.get());

        assertTrue(existsTranlocalField(booleanValue.class, "value"));
        assertFalse(existsField(TransientField.class, "value"));
    }

    @TransactionalObject
    public static class booleanValue {
        private boolean value;

        public booleanValue(boolean value) {
            this.value = value;
        }

        public boolean get() {
            return value;
        }

        public void set(boolean value) {
            this.value = value;
        }
    }

    @Test
    public void shortTest() {
        shortValue value = new shortValue((short) 10);
        assertEquals((short) 10, value.get());

        value.set((short) 20);
        assertEquals((short) 20, value.get());

        assertTrue(existsTranlocalField(shortValue.class, "value"));
        assertFalse(existsField(shortValue.class, "value"));
    }


    @TransactionalObject
    public static class shortValue {
        private short value;

        public shortValue(short value) {
            this.value = value;
        }

        public short get() {
            return value;
        }

        public void set(short value) {
            this.value = value;
        }
    }

    @Test
    public void byteTest() {
        byteValue value = new byteValue((byte) 10);
        assertEquals((byte) 10, value.get());

        value.set((byte) 20);
        assertEquals((byte) 20, value.get());

        assertTrue(existsTranlocalField(byteValue.class, "value"));
        assertFalse(existsField(byteValue.class, "value"));
    }

    @TransactionalObject
    public static class byteValue {
        private byte value;

        public byteValue(byte value) {
            this.value = value;
        }

        public byte get() {
            return value;
        }

        public void set(byte value) {
            this.value = value;
        }
    }

    @Test
    public void charTest() {
        charValue value = new charValue('a');
        assertEquals('a', value.get());

        value.set('b');
        assertEquals('b', value.get());

        assertTrue(existsTranlocalField(charValue.class, "value"));
        assertFalse(existsField(charValue.class, "value"));
    }


    @TransactionalObject
    public static class charValue {
        private char value;

        public charValue(char value) {
            this.value = value;
        }

        public char get() {
            return value;
        }

        public void set(char value) {
            this.value = value;
        }
    }

    @Test
    public void intTest() {
        intValue value = new intValue(10);
        assertEquals(10, value.get());

        value.set(20);
        assertEquals(20, value.get());

        assertTrue(existsTranlocalField(intValue.class, "value"));
        assertFalse(existsField(intValue.class, "value"));
    }


    @TransactionalObject
    public static class intValue {
        private int value;

        public intValue(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void set(int value) {
            this.value = value;
        }
    }

    @Test
    public void floatTest() {
        floatValue value = new floatValue(10);
        assertEquals(10, value.get(), 0.000001);

        value.set(20);
        assertEquals(20, value.get(), 0.000001);

        assertTrue(existsTranlocalField(floatValue.class, "value"));
        assertFalse(existsField(floatValue.class, "value"));
    }

    @TransactionalObject
    public static class floatValue {
        private float value;

        public floatValue(float value) {
            this.value = value;
        }

        public float get() {
            return value;
        }

        public void set(float value) {
            this.value = value;
        }
    }

    @Test
    public void longTest() {
        longValue value = new longValue(10);
        assertEquals(10, value.get());

        value.set(20);
        assertEquals(20, value.get());

        assertTrue(existsTranlocalField(longValue.class, "value"));
        assertFalse(existsField(longValue.class, "value"));
    }

    @TransactionalObject
    public static class longValue {
        private long value;

        public longValue(long value) {
            this.value = value;
        }

        public long get() {
            return value;
        }

        public void set(long value) {
            this.value = value;
        }
    }

    @Test
    public void doubleTest() {
        doubleValue value = new doubleValue(10);
        assertEquals(10, value.get(), 0.000001);

        value.set(20);
        assertEquals(20, value.get(), 0.000001);

        assertTrue(existsTranlocalField(doubleValue.class, "value"));
        assertFalse(existsField(doubleValue.class, "value"));
    }

    @TransactionalObject
    public static class doubleValue {
        private double value;

        public doubleValue(double value) {
            this.value = value;
        }

        public double get() {
            return value;
        }

        public void set(double value) {
            this.value = value;
        }
    }

    @Test
    public void nonTransactionalObjectRefTest() {
        String value1 = "foo";
        NonTransactionalObjectRef ref = new NonTransactionalObjectRef(value1);
        assertEquals(value1, ref.get());

        String value2 = "bar";
        ref.set(value2);
        assertEquals(value2, ref.get());

        assertTrue(existsTranlocalField(NonTransactionalObjectRef.class, "ref"));
        assertFalse(existsField(NonTransactionalObjectRef.class, "ref"));
    }

    @Test
    public void nonTransactionalObjectRefWithNullTest() {
        NonTransactionalObjectRef ref = new NonTransactionalObjectRef(null);
        assertEquals(null, ref.get());

        String value = "foo";
        ref.set(value);
        assertEquals(value, ref.get());

        assertTrue(existsTranlocalField(NonTransactionalObjectRef.class, "ref"));
        assertFalse(existsField(NonTransactionalObjectRef.class, "ref"));
    }

    @TransactionalObject
    public static class NonTransactionalObjectRef {
        private String ref;

        public NonTransactionalObjectRef(String value) {
            this.ref = value;
        }

        public String get() {
            return ref;
        }

        public void set(String value) {
            this.ref = value;
        }
    }

    @Test
    public void transactionalObjectRefTest() {
        intValue value1 = new intValue(10);
        TransactionalObjectRef ref = new TransactionalObjectRef(value1);
        assertSame(value1, ref.get());

        intValue value2 = new intValue(20);
        ref.set(value2);
        assertEquals(value2, ref.get());

        assertTrue(existsTranlocalField(TransactionalObjectRef.class, "ref"));
        assertFalse(existsField(TransactionalObjectRef.class, "ref"));
    }

    @Test
    public void transactionalObjectRefWithNullTest() {
        TransactionalObjectRef ref = new TransactionalObjectRef(null);
        assertEquals(null, ref.get());

        intValue value = new intValue(10);
        ref.set(value);
        assertEquals(value, ref.get());

        assertTrue(existsTranlocalField(TransactionalObjectRef.class, "ref"));
        assertFalse(existsField(TransactionalObjectRef.class, "ref"));
    }

    @TransactionalObject
    public static class TransactionalObjectRef {
        private intValue ref;

        public TransactionalObjectRef(intValue ref) {
            this.ref = ref;
        }

        public intValue get() {
            return ref;
        }

        public void set(intValue value) {
            this.ref = value;
        }
    }

    @Test
    public void runtimeKnownRefType_atomicObjectType() {
        intValue value1 = new intValue(10);
        RuntimeKnownObjectRef ref = new RuntimeKnownObjectRef(value1);
        assertSame(value1, ref.get());

        intValue value2 = new intValue(20);
        ref.set(value2);
        assertEquals(value2, ref.get());

        assertTrue(existsTranlocalField(RuntimeKnownObjectRef.class, "ref"));
        assertFalse(existsField(RuntimeKnownObjectRef.class, "ref"));
    }

    @Test
    public void runtimeKnownRefType_nullValue() {
        TransactionalObjectRef ref = new TransactionalObjectRef(null);
        assertEquals(null, ref.get());

        assertTrue(existsTranlocalField(TransactionalObjectRef.class, "ref"));
        assertFalse(existsField(TransactionalObjectRef.class, "ref"));
    }

    @Test
    public void runtimeKnownRefType_nonAtomicObjectValue() {
        Object value1 = "foo";
        RuntimeKnownObjectRef ref = new RuntimeKnownObjectRef(value1);
        assertSame(value1, ref.get());

        Object value2 = "bar";
        ref.set(value2);
        assertEquals(value2, ref.get());

        assertTrue(existsTranlocalField(RuntimeKnownObjectRef.class, "ref"));
        assertFalse(existsField(RuntimeKnownObjectRef.class, "ref"));
    }

    @TransactionalObject
    public static class RuntimeKnownObjectRef {
        private Object ref;

        public RuntimeKnownObjectRef(Object ref) {
            this.ref = ref;
        }

        public Object get() {
            return ref;
        }

        public void set(Object value) {
            this.ref = value;
        }
    }
}

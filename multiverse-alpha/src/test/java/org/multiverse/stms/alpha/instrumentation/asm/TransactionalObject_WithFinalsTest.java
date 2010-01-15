package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_WithFinalsTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void txObjectWithNoFieldsIsNotManaged() {
        long version = stm.getVersion();

        NoFields noFields = new NoFields();

        assertEquals(version, stm.getVersion());
        assertFalse(noFields instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class NoFields {

        public NoFields() {
        }
    }

    @Test
    public void txObjectWithOneFinalFieldIsNotManaged() {
        long version = stm.getVersion();

        OneFinalField o = new OneFinalField(20);

        assertEquals(version, stm.getVersion());
        assertFalse(o instanceof AlphaTransactionalObject);
        assertEquals(20, o.getValue());
    }

    @TransactionalObject
    public static class OneFinalField {

        final int value;

        public OneFinalField(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void txObjectWithSomeFinalFieldsIsManaged() {
        long version = stm.getVersion();

        SomeFinalFields o = new SomeFinalFields(10, 20);

        assertTrue(o instanceof AlphaTransactionalObject);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(10, o.getFinalValue());
        assertEquals(10, o.finalValue);
        assertEquals(20, o.getNonFinalValue());
    }

    @TransactionalObject
    public static class SomeFinalFields {

        private final int finalValue;
        private int nonFinalValue;

        public SomeFinalFields(int finalValue, int nonFinalValue) {
            this.finalValue = finalValue;
            this.nonFinalValue = nonFinalValue;
        }

        public int getFinalValue() {
            return finalValue;
        }

        public int getNonFinalValue() {
            return nonFinalValue;
        }
    }

    @Test
    public void txObjectWithAllFinalFieldsIsNotManaged() {
        long version = stm.getVersion();

        AllFinalFields o = new AllFinalFields(10, 20, 30);

        assertEquals(version, stm.getVersion());
        assertEquals(10, o.getValue1());
        assertEquals(20, o.getValue2());
        assertEquals(30, o.getValue3());
        assertFalse(o instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class AllFinalFields {

        final int value1;
        final int value2;
        final int value3;

        public AllFinalFields(int value1, int value2, int value3) {
            this.value1 = value1;
            this.value2 = value2;
            this.value3 = value3;
        }

        public int getValue1() {
            return value1;
        }

        public int getValue2() {
            return value2;
        }

        public int getValue3() {
            return value3;
        }
    }

    @Test
    public void testChainedReferences() {
        ChainedRef ref1 = new ChainedRef(null, 1);
        ChainedRef ref2 = new ChainedRef(ref1, 2);
        ChainedRef ref3 = new ChainedRef(ref2, 3);

        assertSame(ref2, ref3.getNext());
        assertSame(ref1, ref3.getIndirectNext());
        assertNull(ref3.getLongIndirectNext());
        assertSame(ref1, ref2.getNext());
        assertNull(ref2.getIndirectNext());
    }

    @TransactionalObject
    public static class ChainedRef {

        int someValue;
        final ChainedRef next;

        public ChainedRef(ChainedRef next, int someValue) {
            this.next = next;
            this.someValue = someValue;
        }

        public ChainedRef getNext() {
            return next;
        }

        public int getSomeValue() {
            return someValue;
        }

        public ChainedRef getIndirectNext() {
            return next.next;
        }

        public ChainedRef getLongIndirectNext() {
            return next.next.next;
        }
    }
}

package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.api.annotations.Exclude;
import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class AtomicObject_ExcludeTest {

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

    @Test
    public void excludeOneOfTheFields() {
        int excludedValue = 10;
        int includedValue = 20;

        long version = stm.getTime();
        ExcludeOneOfTheFields o = new ExcludeOneOfTheFields(excludedValue, includedValue);

        assertEquals(version + 1, stm.getTime());
        assertTrue(o instanceof AlphaAtomicObject);
        assertEquals(excludedValue, o.excluded);
        assertEquals(excludedValue, o.getExcluded());
        assertEquals(includedValue, o.getIncluded());
    }

    @AtomicObject
    public static class ExcludeOneOfTheFields {

        int included;

        @Exclude
        int excluded;

        ExcludeOneOfTheFields(int excluded, int included) {
            this.excluded = excluded;
            this.included = included;
        }

        public int getExcluded() {
            return excluded;
        }

        public int getIncluded() {
            return included;
        }
    }

    @Test
    public void excludeOnlyField() {
        long version = stm.getTime();

        int value = 1;
        ExcludeOnlyField o = new ExcludeOnlyField(value);

        assertFalse(o instanceof AlphaAtomicObject);
        assertEquals(version, stm.getTime());
        assertEquals(1, o.field);
        assertEquals(1, o.getField());
    }

    @AtomicObject
    static class ExcludeOnlyField {

        @Exclude
        int field;

        ExcludeOnlyField(int field) {
            this.field = field;
        }

        public int getField() {
            return field;
        }
    }

    @Test
    public void excludeOnNonTransactionalObjectDoesNotCauseProblems() {
        long version = stm.getTime();

        ExcludeOnNonTransactionalObject o = new ExcludeOnNonTransactionalObject();

        assertEquals(version, stm.getTime());
        assertFalse(o instanceof AlphaAtomicObject);
    }

    static class ExcludeOnNonTransactionalObject {

        @Exclude
        private int field;
    }


    @Test
    public void excludeStaticFieldDoesntCauseProblems() {
        int staticField = 10;
        int instanceField = 20;

        long version = stm.getTime();

        ExcludeStaticField o = new ExcludeStaticField(instanceField, staticField);

        assertEquals(version + 1, stm.getTime());
        assertTrue(o instanceof AlphaAtomicObject);
        assertEquals(staticField, ExcludeStaticField.staticField);
        assertEquals(staticField, ExcludeStaticField.getStaticField());
        assertEquals(staticField, o.getStaticFieldThroughInstance());
        assertEquals(instanceField, o.getInstanceField());
    }

    @AtomicObject
    static class ExcludeStaticField {

        @Exclude
        private static int staticField;

        private int instanceField;

        ExcludeStaticField(int instanceField, int newStaticField) {
            this.instanceField = instanceField;
            staticField = newStaticField;
        }

        public int getInstanceField() {
            return instanceField;
        }

        public static int getStaticField() {
            return staticField;
        }

        public int getStaticFieldThroughInstance() {
            return staticField;
        }
    }
}

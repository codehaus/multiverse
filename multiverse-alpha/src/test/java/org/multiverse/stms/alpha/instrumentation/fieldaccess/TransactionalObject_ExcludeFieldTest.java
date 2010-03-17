package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.Exclude;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_ExcludeFieldTest {

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

    @Test
    public void excludeOneOfTheFields() {
        int excludedValue = 10;
        int includedValue = 20;

        long version = stm.getVersion();
        ExcludeOneOfTheFields o = new ExcludeOneOfTheFields(excludedValue, includedValue);

        assertEquals(version + 1, stm.getVersion());
        assertTrue(o instanceof AlphaTransactionalObject);
        assertEquals(excludedValue, o.excluded);
        assertEquals(excludedValue, o.getExcluded());
        assertEquals(includedValue, o.getIncluded());
    }

    @TransactionalObject
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
        long version = stm.getVersion();

        int value = 1;
        ExcludeOnlyField o = new ExcludeOnlyField(value);

        assertFalse(o instanceof AlphaTransactionalObject);
        assertEquals(version, stm.getVersion());
        assertEquals(1, o.field);
        assertEquals(1, o.getField());
    }

    @TransactionalObject
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
        long version = stm.getVersion();

        ExcludeOnNonTransactionalObject o = new ExcludeOnNonTransactionalObject();

        assertEquals(version, stm.getVersion());
        assertFalse(o instanceof AlphaTransactionalObject);
    }

    static class ExcludeOnNonTransactionalObject {

        @Exclude
        private int field;
    }


    @Test
    public void excludeStaticFieldDoesntCauseProblems() {
        int staticField = 10;
        int instanceField = 20;

        long version = stm.getVersion();

        ExcludeStaticField o = new ExcludeStaticField(instanceField, staticField);

        assertEquals(version + 1, stm.getVersion());
        assertTrue(o instanceof AlphaTransactionalObject);
        assertEquals(staticField, ExcludeStaticField.staticField);
        assertEquals(staticField, ExcludeStaticField.getStaticField());
        assertEquals(staticField, o.getStaticFieldThroughInstance());
        assertEquals(instanceField, o.getInstanceField());
    }

    @TransactionalObject
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

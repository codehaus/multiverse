package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.stms.alpha.AlphaAtomicObject;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class AtomicObject_StaticFieldsTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
        //assertNoInstrumentationProblems();
    }

    @Test
    public void atomicObjectWithOnlyStaticField() {
        long version = stm.getTime();

        AtomicObjectWithOnlyStaticField o = new AtomicObjectWithOnlyStaticField();

        assertEquals(version, stm.getTime());
        assertFalse(o instanceof AlphaAtomicObject);
    }

    @AtomicObject
    public static class AtomicObjectWithOnlyStaticField {

        static int field;

        public AtomicObjectWithOnlyStaticField() {
        }
    }

    @Test
    public void oneOfTheFieldsIsStatic() {
        int instanceValue = 20;
        int staticValue = 100;

        long version = stm.getTime();

        OneOfTheFieldsIsStatic o = new OneOfTheFieldsIsStatic(instanceValue, staticValue);
        assertEquals(version + 1, stm.getTime());
        assertTrue(o instanceof AlphaAtomicObject);
        assertEquals(instanceValue, o.getInstanceField());
        assertEquals(staticValue, o.getStaticFieldThroughInstance());
        assertEquals(staticValue, OneOfTheFieldsIsStatic.staticField);
        assertEquals(staticValue, OneOfTheFieldsIsStatic.getStaticField());
    }

    @AtomicObject
    public static class OneOfTheFieldsIsStatic {

        static int staticField;
        int instanceField;

        public OneOfTheFieldsIsStatic(int instanceValue, int staticValue) {
            this.instanceField = instanceValue;
            staticField = staticValue;
        }

        public int getInstanceField() {
            return instanceField;
        }

        public int getStaticFieldThroughInstance() {
            return staticField;
        }

        public static int getStaticField() {
            return staticField;
        }
    }
}

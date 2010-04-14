package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransactionalObject;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_StaticFieldsTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        clearThreadLocalTransaction();
        //assertNoInstrumentationProblems();
    }

    @Test
    public void txObjectWithOnlyStaticField() {
        long version = stm.getVersion();

        TxObjectWithOnlyStaticField o = new TxObjectWithOnlyStaticField();

        assertEquals(version, stm.getVersion());
        assertFalse(o instanceof AlphaTransactionalObject);
    }

    @TransactionalObject
    public static class TxObjectWithOnlyStaticField {

        static int field;

        public TxObjectWithOnlyStaticField() {
        }
    }

    @Test
    public void oneOfTheFieldsIsStatic() {
        int instanceValue = 20;
        int staticValue = 100;

        long version = stm.getVersion();

        OneOfTheFieldsIsStatic o = new OneOfTheFieldsIsStatic(instanceValue, staticValue);
        assertEquals(version, stm.getVersion());
        assertTrue(o instanceof AlphaTransactionalObject);
        assertEquals(instanceValue, o.getInstanceField());
        assertEquals(staticValue, o.getStaticFieldThroughInstance());
        assertEquals(staticValue, OneOfTheFieldsIsStatic.staticField);
        assertEquals(staticValue, OneOfTheFieldsIsStatic.getStaticField());
    }

    @TransactionalObject
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

package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Stm;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;

public class TransactionalObject_ClashingFieldAndMethodTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
        resetInstrumentationProblemMonitor();
    }

    public void tearDown() {
        resetInstrumentationProblemMonitor();
    }

    @Test
    public void test() throws NoSuchFieldException {
        System.out.println("The following exception is expected");
        //force loading of the class
        System.out.println("ObjectWithClashingField.name: " + ObjectWithClashingField.class);

        long version = stm.getVersion();

        try {
            new ObjectWithClashingField(10);
            fail();
        } catch (IncompatibleClassChangeError expected) {
        }
    }

    @TransactionalObject
    static class ObjectWithClashingField {

        int ___lockOwner;

        public ObjectWithClashingField(int lockOwner) {
            this.___lockOwner = lockOwner;
        }
    }

    @Test
    public void testConflictingMethod() throws NoSuchFieldException {
        //force loading of the class
        System.out.println("ObjectWithClashingMethod.name: " + ObjectWithClashingMethod.class);

        long version = stm.getVersion();

        try {
            ObjectWithClashingMethod o = new ObjectWithClashingMethod();
            o.___getLockOwner();
        } catch (IncompatibleClassChangeError expected) {
        }
    }

    @TransactionalObject
    static class ObjectWithClashingMethod {

        int x;

        public String ___getLockOwner() {
            return null;
        }
    }
}

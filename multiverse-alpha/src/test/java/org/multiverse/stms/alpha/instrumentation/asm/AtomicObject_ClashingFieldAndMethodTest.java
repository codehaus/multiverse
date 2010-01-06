package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.stms.alpha.AlphaStm;

public class AtomicObject_ClashingFieldAndMethodTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @Test
    public void test() throws NoSuchFieldException {
        //force loading of the class
        System.out.println("ObjectWithClashingField.name: " + ObjectWithClashingField.class);

        long version = stm.getTime();

        try {
            new ObjectWithClashingField(10);
            fail();
        } catch (IncompatibleClassChangeError expected) {
        }
    }

    @AtomicObject
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

        long version = stm.getTime();

        try {
            ObjectWithClashingMethod o = new ObjectWithClashingMethod();
            o.___getLockOwner();
        } catch (IncompatibleClassChangeError expected) {
        }
    }

    @AtomicObject
    static class ObjectWithClashingMethod {

        int x;

        public String ___getLockOwner() {
            return null;
        }
    }
}

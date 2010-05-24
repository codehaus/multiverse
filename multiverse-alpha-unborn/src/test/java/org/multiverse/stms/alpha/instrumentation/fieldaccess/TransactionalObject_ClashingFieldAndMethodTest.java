package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Stm;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
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

    /**
     * All the fiels are private, so no worries about conflicts.
     */
    @Test
    public void whenConflictingFieldNothingBadHappens(){
        ObjectWithClashingField o = new ObjectWithClashingField();

        long version = stm.getVersion();

        o.set(10);

        assertEquals(version+1, stm.getVersion());
        assertEquals(10, o.get());
    }

    @TransactionalObject
    static class ObjectWithClashingField {

        int ___lockOwner;

        public void set(int lockOwner) {
            this.___lockOwner = lockOwner;
        }

        public int get() {
            return ___lockOwner;
        }
    }

    @Test
    public void whenConflictingMethod() throws NoSuchFieldException {
        //force loading of the class
        System.out.println("ObjectWithClashingMethod.name: " + ObjectWithClashingMethod.class);

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

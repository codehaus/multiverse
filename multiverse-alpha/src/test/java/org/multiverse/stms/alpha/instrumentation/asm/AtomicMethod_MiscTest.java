package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.templates.AtomicTemplate;

public class AtomicMethod_MiscTest {

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

    public static void assertTransactionWorking() {
        assertIsActive(getThreadLocalTransaction());
    }

    @Test
    public void abstractMethodIsIgnored() {
        testIncomplete();
    }

    public static abstract class AbstractMethod {

        @AtomicMethod
        abstract void doIt();
    }

    //@Test
    //public void nativeMethodIsIgnored() {
    //    testIncomplete();
    //
    //    //NativeMethod nativeMethod = new NativeMethod();
    //    //nativeMethod.doIt();
    //}

    public static class NativeMethod {

        native void doIt();
    }

    /**
     * Tests if the system is able to deal with method that have the same name, but different signatures.
     */
    @Test
    public void clashingAtomicMethodNames() {
        ClashingAtomicMethodNames clashingMethodNames = new ClashingAtomicMethodNames();
        clashingMethodNames.doIt(1);
        clashingMethodNames.doIt(true);
    }

    public class ClashingAtomicMethodNames {

        @AtomicMethod
        public void doIt(boolean b) {
            assertTransactionWorking();
        }

        @AtomicMethod
        public void doIt(int i) {
            assertTransactionWorking();
        }
    }

    @Test
    public void atomicMethodOnAtomicObjectDoesntCauseHarm() {
        AtomicMethodOnAtomicObject o = new AtomicMethodOnAtomicObject();

        long version = stm.getTime();
        o.inc();
        assertEquals(version + 1, stm.getTime());
        assertEquals(11, o.getValue());
    }

    @AtomicObject
    public static class AtomicMethodOnAtomicObject {

        private int value = 10;

        @AtomicMethod
        public void inc() {
            value++;
        }

        public int getValue() {
            return value;
        }
    }

    @Test
    public void atomicObjectCreatedInAtomicMethod() {
        AtomicObjectCreated o = new AtomicObjectCreated();

        long version = stm.getTime();
        o.doit(100);

        assertEquals(version + 1, stm.getTime());
        assertEquals(100, o.getIntRef().get());
    }

    public static class AtomicObjectCreated {

        private IntRef intRef;

        @AtomicMethod
        public void doit(int v) {
            intRef = new IntRef(v);
        }

        public IntRef getIntRef() {
            return intRef;
        }
    }

    @Test
    public void atomicMethodWithAtomicTemplateDoesntCauseHarm() {
        ObjectWithAtomicMethodAndAtomicTemplate o = new ObjectWithAtomicMethodAndAtomicTemplate();

        long version = stm.getTime();
        o.inc();
        assertEquals(version + 1, stm.getTime());
        assertEquals(1, o.getValue());
    }

    @AtomicObject
    public static class ObjectWithAtomicMethodAndAtomicTemplate {

        public int value;

        public ObjectWithAtomicMethodAndAtomicTemplate() {
        }

        @AtomicMethod
        public void inc() {
            //this is an abstract inner class and that is causing the problems
            new AtomicTemplate() {
                @Override
                public Object execute(Transaction t) throws Exception {
                    value++;
                    return null;
                }
            }.execute();
        }

        public int getValue() {
            return value;
        }
    }
}

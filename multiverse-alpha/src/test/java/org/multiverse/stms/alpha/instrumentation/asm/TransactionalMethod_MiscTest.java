package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.transactional.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.templates.TransactionTemplate;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertEquals;
import static org.multiverse.TestUtils.assertIsActive;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalMethod_MiscTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
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

        @TransactionalMethod
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

        @TransactionalMethod
        public void doIt(boolean b) {
            assertTransactionWorking();
        }

        @TransactionalMethod
        public void doIt(int i) {
            assertTransactionWorking();
        }
    }

    @Test
    public void atomicMethodOnAtomicObjectDoesntCauseHarm() {
        AtomicMethodOnAtomicObject o = new AtomicMethodOnAtomicObject();

        long version = stm.getVersion();
        o.inc();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(11, o.getValue());
    }

    @TransactionalObject
    public static class AtomicMethodOnAtomicObject {

        private int value = 10;

        @TransactionalMethod
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

        long version = stm.getVersion();
        o.doit(100);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(100, o.getRef().get());
    }

    public static class AtomicObjectCreated {

        private TransactionalInteger ref;

        @TransactionalMethod
        public void doit(int v) {
            ref = new TransactionalInteger(v);
        }

        public TransactionalInteger getRef() {
            return ref;
        }
    }

    @Test
    public void atomicMethodWithAtomicTemplateDoesntCauseHarm() {
        ObjectWithAtomicMethodAndAtomicTemplate o = new ObjectWithAtomicMethodAndAtomicTemplate();

        long version = stm.getVersion();
        o.inc();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.getValue());
    }

    @TransactionalObject
    public static class ObjectWithAtomicMethodAndAtomicTemplate {

        public int value;

        public ObjectWithAtomicMethodAndAtomicTemplate() {
            value = 0;
        }

        @TransactionalMethod
        public void inc() {
            //this is an abstract inner class and that is causing the problems
            new TransactionTemplate() {
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

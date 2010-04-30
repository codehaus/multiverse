package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.NonTransactional;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.javaagent.JavaAgentProblemMonitor;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.instrumentation.InstrumentationTestUtils.resetInstrumentationProblemMonitor;

public class TransactionalObject_ExcludeMethodTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
        stm = (AlphaStm) getGlobalStmInstance();
        resetInstrumentationProblemMonitor();
    }

    @After
    public void tearDown() {
        assertFalse(JavaAgentProblemMonitor.INSTANCE.isProblemFound());
        resetInstrumentationProblemMonitor();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenExcludeMethodIsCombinedWithExplicitTransactionalMethod_thenTransactionalMethodIgnored() {
        ExcludePriorityObject o = new ExcludePriorityObject();

        //ClassMetadata classMetadata = MetadataRepository.INSTANCE.getClassMetadata(
        //        ClassLoader.getSystemClassLoader(), Type.getType(ExcludePriorityObject.class).getInternalName());
        //boolean isTransactional = classMetadata.getMethodMetadata("excludedIncTwice", "()V").isTransactional();

        // assertFalse(isTransactional);

        long version = stm.getVersion();
        clearThreadLocalTransaction();
        o.excludedIncTwice();
        assertEquals(2, o.get());
        assertEquals(version + 2, stm.getVersion());
    }

    @TransactionalObject
    public static class ExcludePriorityObject {

        private int value;

        public void inc() {

            value++;
        }

        public int get() {
            return value;
        }

        @NonTransactional
        //@TransactionalMethod
        public void excludedIncTwice() {
            inc();
            inc();
        }
    }

    @Test(expected = NoTransactionFoundException.class)
    public void whenExcludedInstanceMethodAccessesTransactionalField_thenNoTransactionFoundException() {
        ObjectForFieldAccess o = new ObjectForFieldAccess();
        o.inc();
    }

    @Test
    public void whenExcludedMethodWithDirectFieldAccessIsWrappedInTransaction_thenNoProblems() {
        ObjectForFieldAccess o = new ObjectForFieldAccess();

        long version = stm.getVersion();
        o.wrappingExcludedInc();
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, o.get());
    }


    @TransactionalObject
    public static class ObjectForFieldAccess {

        private int value;

        @NonTransactional
        public void inc() {
            value++;
        }

        public void wrappingExcludedInc() {
            inc();
        }

        public int get() {
            return value;
        }
    }

    @Test
    public void whenTransactionWrappedAroundExcludedMethod_thenTransactionIsJoined() {
        SomeObject object = new SomeObject();

        long version = stm.getVersion();
        object.wrappingExcludedIncTwice();

        assertEquals(2, object.get());
        assertEquals(version + 1, stm.getVersion());
    }

    @Test
    public void whenMethodExcludedOnNonTransactionalObject_thenIgnored() {
        NonTransactionalObject o = new NonTransactionalObject();
        long version = stm.getVersion();
        o.someMethod();

        assertEquals(1, o.value);
        assertEquals(version, stm.getVersion());
    }

    public static class NonTransactionalObject {
        private int value;

        @NonTransactional
        public void someMethod() {
            value++;
        }
    }

    @Test
    public void whenMultipleTransactionalCallsAreDone_thenMultipleTransactionsStarted() {
        SomeObject object = new SomeObject();

        long version = stm.getVersion();
        object.excludedIncTwice();

        assertEquals(2, object.get());
        assertEquals(version + 2, stm.getVersion());
    }

    @TransactionalObject
    public static class SomeObject {

        private int value;

        public void inc() {
            value++;
        }

        public int get() {
            return value;
        }

        @NonTransactional
        public void excludedIncTwice() {
            inc();
            inc();
        }

        public void wrappingExcludedIncTwice() {
            excludedIncTwice();
        }
    }
}

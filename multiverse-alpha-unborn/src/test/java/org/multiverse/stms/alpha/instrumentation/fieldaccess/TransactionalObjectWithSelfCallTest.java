package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalObjectWithSelfCallTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }


    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void test() {
        Value value = new Value(20);
        value.inc();
        assertEquals(21, value.get());
    }


    @TransactionalObject
    static class Value {

        private int value;

        Value(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void inc() {
            set(get() + 1);
        }

        public void set(int value) {
            this.value = value;
        }
    }
}

package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_ChainOfMethodsTest {

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
    public void chainOfMethodsOfSameType() {
        SomeTypeMethodChain ref1 = new SomeTypeMethodChain(null);
        SomeTypeMethodChain ref2 = new SomeTypeMethodChain(ref1);
        SomeTypeMethodChain ref3 = new SomeTypeMethodChain(ref2);

        assertNull(ref1.getNext());
        assertSame(ref1, ref2.getNext());
        assertNull(ref2.getNextNext());
        assertSame(ref2, ref3.getNext());
        assertSame(ref1, ref3.getNextNext());
        assertNull(ref3.getNextNextNext());
    }


    @TransactionalObject
    private static class SomeTypeMethodChain {
        SomeTypeMethodChain next;

        private SomeTypeMethodChain(SomeTypeMethodChain next) {
            this.next = next;
        }

        public SomeTypeMethodChain getNextNext() {
            return getNext().getNext();
        }

        public SomeTypeMethodChain getNextNextNext() {
            return getNext().getNext().getNext();
        }

        public SomeTypeMethodChain getNext() {
            return next;
        }

        public void setNext(SomeTypeMethodChain next) {
            this.next = next;
        }
    }
}

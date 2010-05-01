package org.multiverse.integrationtests.failureatomicity;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalObject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * Test that checks that a transaction is rolled back if an exception is thrown.
 *
 * @author Peter Veentjer
 */
public class ExceptionRollbackTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenCheckedException_thenRollback() {
        Ref ref = new Ref(10);

        try {
            ref.setAndThrowChecked(20);
            fail();
        } catch (CheckedException ex) {
        }

        assertEquals(10, ref.get());
    }

    @Test
    public void whenUncheckedException_thenRollback() {
        Ref ref = new Ref(10);

        try {
            ref.setAndThrowUnchecked(20);
            fail();
        } catch (UncheckedException ex) {
        }

        assertEquals(10, ref.get());
    }

    @Test
    public void whenNonControlFlowError_thenRollback() {
        Ref ref = new Ref(10);

        try {
            ref.setAndThrowNonControlFlowError(20);
            fail();
        } catch (NonControlFlowError ex) {
        }

        assertEquals(10, ref.get());
    }

    @TransactionalObject
    public class Ref {
        int value;

        public Ref(int value) {
            this.value = value;
        }

        public int get() {
            return value;
        }

        public void setAndThrowChecked(int newValue) throws CheckedException {
            this.value = newValue;
            throw new CheckedException();
        }

        public void setAndThrowUnchecked(int newValue) {
            this.value = newValue;
            throw new UncheckedException();
        }

        public void setAndThrowNonControlFlowError(int newValue) {
            this.value = newValue;
            throw new NonControlFlowError();
        }
    }

    class CheckedException extends Exception {
    }

    class UncheckedException extends RuntimeException {
    }

    class NonControlFlowError extends Error {
    }
}

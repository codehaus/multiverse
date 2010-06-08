package org.multiverse.stms.alpha.instrumentation.integrationtest;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TryCatchTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenUsedInCatchClause() {
        IntRef ref = new IntRef();
        methodWhereExceptionCaught(ref);
        assertEquals(1, ref.get());
    }

    @TransactionalMethod(readonly = false)
    public void methodWhereExceptionCaught(IntRef ref) {
        try {
            throw new Exception();
        } catch (Exception ex) {
            ref.set(1);
        }
    }

    @Test
    public void whenUsedInFinallyClauseWithoutException() {
        IntRef ref = new IntRef();
        methodWithFinallyClause(false, ref);
        assertEquals(1, ref.get());
    }

    @Test
    @Ignore
    public void whenUsedInFinallyClauseWithException() {
        IntRef ref = new IntRef();

        try {
            methodWithFinallyClause(true, ref);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertEquals(1, ref.get());
    }

    @TransactionalMethod(readonly = false)
    public void methodWithFinallyClause(boolean error, IntRef ref) {
        try {
            if (error) {
                throw new ExpectedRuntimeException();
            }
        } finally {
            ref.set(1);
            getThreadLocalTransaction().commit();
        }
    }

}


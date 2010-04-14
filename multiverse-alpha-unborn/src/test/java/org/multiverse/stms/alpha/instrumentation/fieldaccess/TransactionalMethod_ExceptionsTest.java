package org.multiverse.stms.alpha.instrumentation.fieldaccess;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import java.io.FileNotFoundException;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalMethod_ExceptionsTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void checkedExceptionIsPropagated() {
        Exception ex = new FileNotFoundException();

        CheckedExceptionIsPropagated r = new CheckedExceptionIsPropagated();
        r.exception = ex;

        long version = stm.getVersion();
        try {
            r.doIt();
            fail();
        } catch (Exception found) {
            assertSame(ex, found);
        }

        assertEquals(version, stm.getVersion());
        assertEquals(0, r.ref.get());
    }

    public static class CheckedExceptionIsPropagated {

        Exception exception;
        TransactionalInteger ref = new TransactionalInteger(0);

        @TransactionalMethod
        public void doIt() throws Exception {
            ref.inc();
            throw exception;
        }
    }

    @Test
    public void runtimeExceptionIsPropagated() {
        RuntimeException ex = new IllegalArgumentException();

        RuntimeExceptionIsPropagated r = new RuntimeExceptionIsPropagated();

        r.exception = ex;

        long version = stm.getVersion();
        try {
            r.doIt();
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertEquals(version, stm.getVersion());
        assertEquals(0, r.ref.get());
    }

    public static class RuntimeExceptionIsPropagated {

        RuntimeException exception;
        TransactionalInteger ref = new TransactionalInteger(0);

        @TransactionalMethod
        public void doIt() {
            ref.inc();
            throw exception;
        }
    }
}


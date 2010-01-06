package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.datastructures.refs.IntRef;
import org.multiverse.stms.alpha.AlphaStm;

import java.io.FileNotFoundException;

public class AtomicMethod_ExceptionsTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = AlphaStm.createDebug();
        setGlobalStmInstance(stm);
    }

    @Test
    public void checkedExceptionIsPropagated() {
        Exception ex = new FileNotFoundException();

        CheckedExceptionIsPropagated r = new CheckedExceptionIsPropagated();
        r.exception = ex;

        long version = stm.getTime();
        try {
            r.doIt();
            fail();
        } catch (Exception found) {
            assertSame(ex, found);
        }

        assertEquals(version, stm.getTime());
        assertEquals(0, r.ref.get());
    }

    public static class CheckedExceptionIsPropagated {

        Exception exception;
        IntRef ref = new IntRef(0);

        @AtomicMethod
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

        long version = stm.getTime();
        try {
            r.doIt();
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertEquals(version, stm.getTime());
        assertEquals(0, r.ref.get());
    }

    public static class RuntimeExceptionIsPropagated {

        RuntimeException exception;
        IntRef ref = new IntRef(0);

        @AtomicMethod
        public void doIt() {
            ref.inc();
            throw exception;
        }
    }
}


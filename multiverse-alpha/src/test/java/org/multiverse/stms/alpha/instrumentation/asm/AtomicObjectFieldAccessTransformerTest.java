package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.stms.alpha.AlphaStm;

/**
 * @author Peter Veentjer
 */
public class AtomicObjectFieldAccessTransformerTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    @Test
    public void fieldAccessWithoutTransaction() {
        SomeRef someRef = new SomeRef();
        long version = stm.getTime();
        try {
            int x = someRef.x;
            fail();
        } catch (NoTransactionFoundException ignore) {
        }

        assertEquals(version, stm.getTime());
    }

    @Test
    public void fieldAccessWithTransaction() {
        SomeRef someRef = new SomeRef();
        long version = stm.getTime();
        int x = inc(someRef);

        assertEquals(11, x);
        assertEquals(version + 1, stm.getTime());
    }

    @AtomicMethod
    public int inc(SomeRef someRef) {
        someRef.x++;
        return someRef.x;
    }

    @AtomicObject
    public class SomeRef {

        public int x = 10;
    }
}

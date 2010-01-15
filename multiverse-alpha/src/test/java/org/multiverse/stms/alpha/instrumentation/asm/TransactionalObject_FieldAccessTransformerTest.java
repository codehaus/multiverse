package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.exceptions.NoTransactionFoundException;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

/**
 * @author Peter Veentjer
 */
public class TransactionalObject_FieldAccessTransformerTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @Test
    public void fieldAccessWithoutTransaction() {
        SomeRef someRef = new SomeRef();
        long version = stm.getVersion();
        try {
            int x = someRef.x;
            fail();
        } catch (NoTransactionFoundException ignore) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void fieldAccessWithTransaction() {
        SomeRef someRef = new SomeRef();
        long version = stm.getVersion();
        int x = inc(someRef);

        assertEquals(11, x);
        assertEquals(version + 1, stm.getVersion());
    }

    @TransactionalMethod
    public int inc(SomeRef someRef) {
        someRef.x++;
        return someRef.x;
    }

    @TransactionalObject
    public class SomeRef {

        public int x = 10;
    }
}

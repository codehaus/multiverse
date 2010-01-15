package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.transactional.primitives.TransactionalInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class ObjectWithTransactionalMethodTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
    }

    @After
    public void tearDown() {
        //assertNoInstrumentationProblems();
    }

    @Test
    public void test() {
        ObjectWithTransactionalMethod o = new ObjectWithTransactionalMethod();
        o.theMethod();

        assertEquals(1, o.ref.get());
    }

    public class ObjectWithTransactionalMethod {
        private final TransactionalInteger ref = new TransactionalInteger(0);

        @TransactionalMethod
        public void theMethod() {
            assertNotNull(getThreadLocalTransaction());
            ref.inc();
        }
    }
}

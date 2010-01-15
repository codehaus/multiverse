package org.multiverse.stms.alpha.instrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.AlphaStm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalClassTransformerIntegrationTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void testInstanceMethod() {
        LongRef v1 = new LongRef(0);

        inc(v1);

        assertEquals(1, v1.get());
    }

    @TransactionalMethod
    public void inc(LongRef v1) {
        v1.inc();
    }

    @Test
    public void testStaticMethod() {
        LongRef v1 = new LongRef(0);

        incStatic(v1);

        assertEquals(1, v1.get());
    }

    @TransactionalMethod
    public static void incStatic(LongRef v1) {
        v1.inc();
    }

    @Test
    public void testMultipleUpdates() {
        LongRef v1 = new LongRef(0);
        LongRef v2 = new LongRef(1);
        LongRef v3 = new LongRef(2);

        long clockVersion = stm.getVersion();

        updateToStmClockVersion(v1, v2, v3);

        assertEquals(clockVersion, v1.get());
        assertEquals(clockVersion, v2.get());
        assertEquals(clockVersion, v3.get());
    }

    @TransactionalMethod
    public void updateToStmClockVersion(LongRef... values) {
        for (LongRef value : values) {
            value.set(stm.getVersion());
        }
    }
}

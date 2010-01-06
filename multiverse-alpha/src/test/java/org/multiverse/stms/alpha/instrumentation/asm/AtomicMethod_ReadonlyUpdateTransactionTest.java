package org.multiverse.stms.alpha.instrumentation.asm;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import org.multiverse.api.annotations.AtomicMethod;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransaction;
import org.multiverse.stms.alpha.ReadonlyAlphaTransaction;

public class AtomicMethod_ReadonlyUpdateTransactionTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        clearThreadLocalTransaction();
    }

    @Test
    public void instanceReadonlyMethod() {
        long version = stm.getTime();

        InstanceReadonlyMethod method = new InstanceReadonlyMethod();
        method.execute();

        AlphaTransaction transaction = method.transaction;

        assertTrue(transaction instanceof ReadonlyAlphaTransaction);
        assertEquals(version, stm.getTime());
        assertNotNull(transaction);
        assertEquals(version, method.version);
    }

    static class InstanceReadonlyMethod {

        AlphaTransaction transaction;
        long version;

        @AtomicMethod(readonly = true)
        public void execute() {
            transaction = (AlphaTransaction) getThreadLocalTransaction();
            version = transaction.getReadVersion();
        }
    }

    @Test
    public void staticReadonlyMethod() {
        long version = stm.getTime();

        StaticReadonlyMethod.execute();

        AlphaTransaction transaction = StaticReadonlyMethod.transaction;

        assertEquals(version, stm.getTime());
        assertNotNull(transaction);
        assertEquals(version, StaticReadonlyMethod.version);
        assertTrue(transaction instanceof ReadonlyAlphaTransaction);
    }

    static class StaticReadonlyMethod {

        static AlphaTransaction transaction;
        static long version;

        @AtomicMethod(readonly = true)
        public static void execute() {
            transaction = (AlphaTransaction) getThreadLocalTransaction();
            version = transaction.getReadVersion();
        }
    }
}

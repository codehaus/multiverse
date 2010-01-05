package org.multiverse.stms.alpha.instrumentation.asm;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.transactional.annotations.TransactionalMethod;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.NonTrackingReadonlyAlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionalMethod_ReadonlyUpdateTransactionTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void instanceReadonlyMethod() {
        long version = stm.getVersion();

        InstanceReadonlyMethod method = new InstanceReadonlyMethod();
        method.execute();

        AlphaTransaction transaction = method.transaction;

        assertTrue(transaction instanceof NonTrackingReadonlyAlphaTransaction);
        assertEquals(version, stm.getVersion());
        assertNotNull(transaction);
        assertEquals(version, method.version);
    }

    static class InstanceReadonlyMethod {

        AlphaTransaction transaction;
        long version;

        @TransactionalMethod(readonly = true)
        public void execute() {
            transaction = (AlphaTransaction) getThreadLocalTransaction();
            version = transaction.getReadVersion();
        }
    }

    @Test
    public void staticReadonlyMethod() {
        long version = stm.getVersion();

        StaticReadonlyMethod.execute();

        AlphaTransaction transaction = StaticReadonlyMethod.transaction;

        assertEquals(version, stm.getVersion());
        assertNotNull(transaction);
        assertEquals(version, StaticReadonlyMethod.version);
        assertTrue(transaction instanceof NonTrackingReadonlyAlphaTransaction);
    }

    static class StaticReadonlyMethod {

        static AlphaTransaction transaction;
        static long version;

        @TransactionalMethod(readonly = true)
        public static void execute() {
            transaction = (AlphaTransaction) getThreadLocalTransaction();
            version = transaction.getReadVersion();
        }
    }
}

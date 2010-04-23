package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.transactions.readonly.AbstractReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.readonly.NonTrackingReadonlyAlphaTransaction;
import org.multiverse.stms.alpha.transactions.update.AbstractUpdateAlphaTransaction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;

public class TransactionSelectionTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void test() {
        SomeAtomicObject object = new SomeAtomicObject();
        object.noAnnotations();
        object.readOnly();
        object.readOnlyAndTrackReads();
        object.updateTracking();
        object.updateNonTracking();
    }

    @TransactionalObject
    public class SomeAtomicObject {

        private int somefield;

        public SomeAtomicObject() {
            this.somefield = 1;
        }

        public void noAnnotations() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx instanceof NonTrackingReadonlyAlphaTransaction);
        }

        @TransactionalMethod(readonly = true)
        public void readOnly() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx instanceof AbstractReadonlyAlphaTransaction);
        }

        @TransactionalMethod(readonly = true, trackReads = true)
        public void readOnlyAndTrackReads() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx instanceof AbstractReadonlyAlphaTransaction);
        }

        @TransactionalMethod(readonly = false, trackReads = true)
        public void updateTracking() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfiguration().isReadonly());
            assertTrue(tx.getConfiguration().isReadTrackingEnabled());
            assertTrue(tx instanceof AbstractUpdateAlphaTransaction);
        }

        @TransactionalMethod(trackReads = false)
        public void updateNonTracking() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx instanceof NonTrackingReadonlyAlphaTransaction);
        }
    }
}

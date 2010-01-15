package org.multiverse.stms.alpha;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.annotations.TransactionalMethod;
import org.multiverse.annotations.TransactionalObject;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.transactions.readonly.AbstractReadonlyAlphaTransaction;
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
            assertTrue(tx instanceof AbstractUpdateAlphaTransaction);
        }

        @TransactionalMethod(readonly = true)
        public void readOnly() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx instanceof AbstractReadonlyAlphaTransaction);
        }

        @TransactionalMethod(readonly = true, automaticReadTracking = true)
        public void readOnlyAndTrackReads() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx instanceof AbstractReadonlyAlphaTransaction);
        }

        @TransactionalMethod(readonly = false, automaticReadTracking = true)
        public void updateTracking() {
            Transaction tx = getThreadLocalTransaction();
            assertFalse(tx.getConfig().isReadonly());
            assertTrue(tx.getConfig().automaticReadTracking());
            assertTrue(tx instanceof AbstractUpdateAlphaTransaction);
        }

        @TransactionalMethod(automaticReadTracking = false)
        public void updateNonTracking() {
            Transaction tx = getThreadLocalTransaction();
            assertTrue(tx instanceof AbstractUpdateAlphaTransaction);
        }
    }
}

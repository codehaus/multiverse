package org.multiverse.stms.alpha;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.multiverse.DummyTransaction;
import static org.multiverse.TestUtils.assertIsAborted;
import static org.multiverse.TestUtils.assertIsCommitted;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;
import org.multiverse.api.annotations.AtomicObject;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.FailedToObtainLocksException;
import org.multiverse.api.exceptions.WriteConflictException;
import org.multiverse.stms.alpha.manualinstrumentation.IntRef;
import org.multiverse.stms.alpha.manualinstrumentation.IntRefTranlocal;
import org.multiverse.templates.AbortedException;
import org.multiverse.templates.AtomicTemplate;

public class UpdateAlphaTransaction_commitTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
        setThreadLocalTransaction(null);
    }

    @After
    public void tearDown() {
        setThreadLocalTransaction(null);
    }

    public AlphaTransaction startUpdateTransaction() {
        AlphaTransaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testCount() {
        final TestObject object = new TestObject();

        AtomicTemplate<Object> at = new AtomicTemplate<Object>() {
            @Override
            public Object execute(Transaction t) throws Exception {
                object.incCount();
                assertEquals(1, object.getCount());
                t.abort();
                return null;
            }
        };

        try {
            at.execute();
            fail();
        } catch (AbortedException expected) {
        }

        assertEquals(0, object.getCount());
    }

    @AtomicObject
    public static class TestObject {

        private int count = 0;

        public void incCount() {
            count += 1;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int c) {
            count = c;
        }
    }

    // ================== commit =============================

    @Test
    public void commitFailsIfWriteConflictIsEncountered() {
        IntRef ref = new IntRef(0);

        AlphaTransaction t1 = stm.startUpdateTransaction(null);
        IntRefTranlocal tranlocalIntValueR1 = (IntRefTranlocal) t1.load(ref);

        AlphaTransaction t2 = stm.startUpdateTransaction(null);
        IntRefTranlocal tranlocalIntValueR2 = (IntRefTranlocal) t2.load(ref);
        ref.inc(tranlocalIntValueR2);
        t2.commit();

        long version = stm.getTime();
        IntRefTranlocal committed = (IntRefTranlocal) ref.___load(version);

        ref.inc(tranlocalIntValueR1);

        try {
            t1.commit();
            fail();
        } catch (WriteConflictException e) {
        }

        assertIsAborted(t1);
        assertSame(committed, ref.___load(version));
        assertEquals(version, stm.getTime());
    }

    @Test
    public void commitFailsIfLocksCantBeAcquired() {
        IntRef intValue = new IntRef(0);

        long version = stm.getTime();

        Transaction t = startUpdateTransaction();
        intValue.inc();

        Transaction otherOwner = new DummyTransaction();
        intValue.___tryLock(otherOwner);

        try {
            t.commit();
            fail();
        } catch (FailedToObtainLocksException e) {
        }

        setThreadLocalTransaction(null);
        intValue.___releaseLock(otherOwner);

        assertIsAborted(t);
        assertEquals(version, stm.getTime());
        assertEquals(0, intValue.get());
    }

    @Test
    public void commitUnusedStartedTransaction() {
        long startVersion = stm.getTime();

        Transaction t1 = startUpdateTransaction();
        t1.commit();

        assertEquals(startVersion, stm.getTime());
        assertIsCommitted(t1);
    }

    @Test
    public void commitChangesOnReadPrivatizedObjects() {
        Transaction t1 = startUpdateTransaction();
        IntRef value = new IntRef(10);
        t1.commit();

        long startVersion = stm.getTime();
        Transaction t2 = startUpdateTransaction();
        value.inc();
        t2.commit();

        IntRefTranlocal stored = (IntRefTranlocal) value.___load(stm.getTime());
        assertIsCommitted(t2);
        assertEquals(startVersion + 1, stm.getTime());
        assertEquals(11, stored.value);
        assertEquals(stm.getTime(), stored.___writeVersion);
        assertEquals(value, stored.getAtomicObject());
    }

    @Test
    public void commitChangesOnAttachAsNewObjects() {
        Transaction t1 = startUpdateTransaction();

        long startVersion = stm.getTime();
        IntRef value = new IntRef(10);
        t1.commit();

        assertIsCommitted(t1);
        assertEquals(startVersion + 1, stm.getTime());

        IntRefTranlocal stored = (IntRefTranlocal) value.___load(stm.getTime());
        assertEquals(10, stored.value);
        assertEquals(stm.getTime(), stored.___writeVersion);
        assertEquals(value, stored.getAtomicObject());
    }

    @Test
    public void commitNoDirtyChanges() {
        IntRef intValue = new IntRef(0);

        long startVersion = stm.getTime();

        Transaction t = startUpdateTransaction();
        intValue.get();
        t.commit();

        assertIsCommitted(t);
        //this part is going to work when the isDirty functionality is added.
        assertEquals(startVersion, stm.getTime());
    }

    @Test
    public void commitComplexScenario() {
        IntRef v1 = new IntRef(1);
        IntRef v2 = new IntRef(2);
        IntRef v3 = new IntRef(3);

        long startVersion = stm.getTime();

        Transaction t = startUpdateTransaction();
        v1.inc();
        v2.inc();
        v3.inc();
        IntRef v4 = new IntRef(4);
        IntRef v5 = new IntRef(5);
        IntRef v6 = new IntRef(6);
        t.commit();

        setThreadLocalTransaction(null);

        assertEquals(startVersion + 1, stm.getTime());
        assertIsCommitted(t);
        assertEquals(2, v1.get());
        assertEquals(3, v2.get());
        assertEquals(4, v3.get());
        assertEquals(4, v4.get());
        assertEquals(5, v5.get());
        assertEquals(6, v6.get());
    }

    @Test
    public void commitOnCommittedTransactionIsIgnored() {
        IntRef intValue = new IntRef(1);

        Transaction t = startUpdateTransaction();
        intValue.inc();
        t.commit();

        long version = stm.getTime();
        t.commit();

        setThreadLocalTransaction(null);

        assertIsCommitted(t);
        assertEquals(version, stm.getTime());
        assertEquals(2, intValue.get());
    }

    @Test
    public void commitOnAbortedTransactionFails() {
        IntRef value = new IntRef(1);

        Transaction t = startUpdateTransaction();
        value.inc();
        t.abort();

        long version = stm.getTime();
        try {
            t.commit();
            fail();
        } catch (DeadTransactionException ex) {
        }

        setThreadLocalTransaction(null);

        assertIsAborted(t);
        assertEquals(version, stm.getTime());
        assertEquals(1, value.get());
    }
}

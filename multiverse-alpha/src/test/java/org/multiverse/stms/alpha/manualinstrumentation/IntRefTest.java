package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.getThreadLocalTransaction;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.alpha.AlphaTestUtils.startTrackingUpdateTransaction;

public class IntRefTest {

    private AlphaStm stm;

    @Before
    public void setUp() {
        stm = (AlphaStm) getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    public AlphaTransaction startTransaction() {
        AlphaTransaction t = startTrackingUpdateTransaction(stm);
        setThreadLocalTransaction(t);
        return t;
    }

    // =========================== is dirty =========================================

    @Test
    public void isDirty_freshObject() {
        AlphaTransaction t = startTransaction();
        IntRef value = new IntRef(0);
        IntRefTranlocal tranlocal = (IntRefTranlocal) t.openForWrite(value);
        assertTrue(tranlocal.isDirty());
    }

    @Test
    public void isDirty_readonlyObject() {
        IntRef value = new IntRef(0);

        AlphaTransaction t = startTransaction();
        IntRefTranlocal tranlocal = (IntRefTranlocal) t.openForWrite(value);
        assertFalse(tranlocal.isDirty());
    }

    @Test
    public void isDirty_dirtyObject() {
        IntRef value = new IntRef(0);

        AlphaTransaction t = startTransaction();
        value.inc();
        IntRefTranlocal tranlocal = (IntRefTranlocal) t.openForWrite(value);

        assertTrue(tranlocal.isDirty());
    }

    @Test
    public void hasConflict_withConflict() {
        IntRef value = new IntRef(0);

        AlphaTransaction t1 = startTransaction();
        value.inc();

        AlphaTransaction t2 = startTransaction();
        value.inc();
        t2.commit();
        setThreadLocalTransaction(t1);

        IntRefTranlocal tranlocal = (IntRefTranlocal) t1.openForWrite(value);
        assertTrue(tranlocal.hasConflict());
    }

    // ========================= atomic behavior ====================================

    @Test
    public void atomicCreation() {
        long startVersion = stm.getVersion();

        IntRef intValue = new IntRef(10);

        assertEquals(startVersion + 1, stm.getVersion());
        assertNull(getThreadLocalTransaction());
        assertEquals(10, intValue.get());
    }

    @Test
    public void atomicGet() {
        IntRef intValue = new IntRef(10);

        long startVersion = stm.getVersion();
        int result = intValue.get();
        assertEquals(10, result);
        assertNull(getThreadLocalTransaction());
        assertEquals(startVersion, stm.getVersion());
    }

    @Test
    public void atomicSet() {
        IntRef intValue = new IntRef(10);

        long startVersion = stm.getVersion();

        intValue.set(100);

        assertNull(getThreadLocalTransaction());
        assertEquals(startVersion + 1, stm.getVersion());
        assertEquals(100, intValue.get());
    }

    @Test
    public void atomicInc() {
        IntRef intValue = new IntRef(10);

        long startVersion = stm.getVersion();

        intValue.inc();

        assertNull(getThreadLocalTransaction());
        assertEquals(startVersion + 1, stm.getVersion());
        assertEquals(11, intValue.get());
    }

    // ========================= non atomic behavior =============================

    @Test
    public void existingTransaction() {
        Transaction t1 = startTransaction();
        IntRef intValue = new IntRef(10);
        t1.commit();

        Transaction t2 = startTransaction();
        assertEquals(10, intValue.get());

        intValue.inc();
        assertEquals(11, intValue.get());
        t2.commit();
    }

    @Test
    public void existingTransaction2() {
        Transaction t1 = startTransaction();
        IntRef intValue = new IntRef(10);
        t1.commit();

        Transaction t2 = startTransaction();
        assertEquals(10, intValue.get());
    }

    @Test
    public void testSingleTransaction() {
        Transaction t1 = startTransaction();
        IntRef intValue = new IntRef(10);
        intValue.inc();
        assertEquals(11, intValue.get());
        t1.commit();

        Transaction t2 = startTransaction();
        assertEquals(11, intValue.get());
    }

    @Test
    public void testRollback() {
        Transaction t1 = startTransaction();
        IntRef intValue = new IntRef(10);
        t1.commit();

        Transaction t2 = startTransaction();
        intValue.inc();
        t2.abort();

        Transaction t3 = startTransaction();
        assertEquals(10, intValue.get());
    }
}

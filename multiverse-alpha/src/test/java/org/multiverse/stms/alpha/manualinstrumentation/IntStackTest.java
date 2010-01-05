package org.multiverse.stms.alpha.manualinstrumentation;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.transactions.AlphaTransaction;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import static org.multiverse.stms.alpha.AlphaTestUtils.startTrackingUpdateTransaction;

public class IntStackTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        stm = (AlphaStm) getGlobalStmInstance();
    }

    public AlphaTransaction startTransaction() {
        AlphaTransaction t = startTrackingUpdateTransaction(stm);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testNewStackIsDirtyByDefault() {
        AlphaTransaction t = startTransaction();
        IntStack intStack = new IntStack();
        IntStackTranlocal tranlocal = (IntStackTranlocal) t.openForWrite(intStack);
        assertTrue(tranlocal.isDirty());
    }

    @Test
    public void openedForWriteStackIsNotDirty() {
        IntStack intStack = new IntStack();

        AlphaTransaction t = startTransaction();
        IntStackTranlocal tranlocal = (IntStackTranlocal) t.openForWrite(intStack);
        assertFalse(tranlocal.isDirty());
    }

    @Test
    public void modifiedStackIsDirty() {
        IntStack intStack = new IntStack();

        AlphaTransaction t = startTransaction();
        intStack.push(1);
        IntStackTranlocal tranlocal = (IntStackTranlocal) t.openForWrite(intStack);

        assertTrue(tranlocal.isDirty());
    }

    @Test
    public void testEmptyStack() {
        AlphaTransaction t1 = startTransaction();
        IntStack intStack = new IntStack();
        assertTrue(intStack.isEmpty());
        t1.commit();

        AlphaTransaction t2 = startTransaction();
        assertTrue(intStack.isEmpty());
    }

    @Test
    public void testNonEmptyStack() {
        AlphaTransaction t1 = startTransaction();
        IntStack intStack = new IntStack();
        intStack.push(5);
        intStack.push(10);
        assertEquals(2, intStack.size());
        t1.commit();

        AlphaTransaction t2 = startTransaction();
        assertEquals(2, intStack.size());
        assertEquals(10, intStack.pop());
        assertEquals(5, intStack.pop());
    }

    @Test
    public void testRollback() {
        AlphaTransaction t1 = startTransaction();
        IntStack intStack = new IntStack();
        intStack.push(10);
        t1.commit();

        AlphaTransaction t2 = startTransaction();
        assertEquals(10, intStack.pop());
        t2.abort();

        AlphaTransaction t3 = startTransaction();
        assertEquals(1, intStack.size());
        assertEquals(10, intStack.pop());
    }

    @Test
    public void testPushAndPop() {
        AlphaTransaction t1 = startTransaction();
        IntStack intStack = new IntStack();
        t1.commit();

        AlphaTransaction t2 = startTransaction();
        intStack.push(1);
        t2.commit();

        AlphaTransaction t3 = startTransaction();
        int popped = intStack.pop();
        t3.commit();

        assertEquals(1, popped);
    }
}

package org.multiverse.stms.alpha.manualinstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.stms.alpha.AlphaStm;
import org.multiverse.stms.alpha.AlphaTransaction;
import org.multiverse.stms.alpha.DirtinessStatus;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class IntStackTest {
    private AlphaStm stm;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    public AlphaTransaction startTransaction() {
        AlphaTransaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testNewStackIsDirtyByDefault() {
        AlphaTransaction t = startTransaction();
        IntStack intStack = new IntStack();
        IntStackTranlocal tranlocalIntStack = (IntStackTranlocal) t.load(intStack);
        assertEquals(DirtinessStatus.fresh, tranlocalIntStack.getDirtinessStatus());
    }

    @Test
    public void loadedStackIsNotDirty() {
        IntStack intStack = new IntStack();

        AlphaTransaction t = startTransaction();
        IntStackTranlocal tranlocalIntStack = (IntStackTranlocal) t.load(intStack);
        assertEquals(DirtinessStatus.clean, tranlocalIntStack.getDirtinessStatus());
    }

    @Test
    public void modifiedStackIsDirty() {
        IntStack intStack = new IntStack();

        AlphaTransaction t = startTransaction();
        intStack.push(1);
        IntStackTranlocal tranlocalIntStack = (IntStackTranlocal) t.load(intStack);

        assertEquals(DirtinessStatus.dirty, tranlocalIntStack.getDirtinessStatus());
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

package org.multiverse.stms.alpha.manualinstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.setGlobalStmInstance;
import org.multiverse.api.Stm;
import org.multiverse.api.Transaction;
import org.multiverse.stms.alpha.AlphaStm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class StackTest {
    private Stm stm;

    @Before
    public void setUp() {
        setThreadLocalTransaction(null);
        stm = new AlphaStm();
        setGlobalStmInstance(stm);
    }

    public Transaction startUpdateTransaction() {
        Transaction t = stm.startUpdateTransaction(null);
        setThreadLocalTransaction(t);
        return t;
    }

    @Test
    public void testClearEmptyStack() {
        Stack stack = new Stack();
        stack.clear();

        assertEquals(0, stack.size());
        assertTrue(stack.isEmpty());
    }

    @Test
    public void testClearNonEmptyStack() {
        Stack<String> stack = new Stack<String>();
        stack.push("a");
        stack.push("b");
        stack.push("c");
        stack.clear();

        assertEquals(0, stack.size());
        assertTrue(stack.isEmpty());
    }

    @Test
    public void testEmptyStack() {
        Transaction t1 = startUpdateTransaction();
        Stack stack = new Stack();
        assertTrue(stack.isEmpty());
        t1.commit();

        Transaction t2 = startUpdateTransaction();
        assertTrue(stack.isEmpty());
    }

    @Test
    public void testNonEmpty() {
        Transaction t1 = startUpdateTransaction();
        IntRef v1 = new IntRef(10);
        IntRef v2 = new IntRef(10);
        Stack stack = new Stack();
        stack.push(v1);
        stack.push(v2);
        t1.commit();
    }

    @Test
    public void test() {
        IntRef v1 = new IntRef(10);
        IntRef v2 = new IntRef(10);

        Transaction t2 = startUpdateTransaction();
        Stack stack = new Stack();
        stack.push(v1);
        stack.push(v2);
        t2.commit();
    }

    @Test
    public void testAnotherScenario() {
        Transaction t1 = startUpdateTransaction();
        IntRef v1 = new IntRef(10);
        IntRef v2 = new IntRef(10);
        t1.commit();

        Transaction t2 = startUpdateTransaction();
        v1.inc();
        Stack stack = new Stack();
        stack.push(v1);
        stack.push(v2);
        t2.commit();

        Transaction t3 = startUpdateTransaction();
        assertEquals(11, v1.get());
        assertEquals(10, v2.get());
    }
}

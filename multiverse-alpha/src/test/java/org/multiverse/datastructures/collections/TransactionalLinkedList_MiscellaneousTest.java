package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;
import org.multiverse.api.Transaction;

public class TransactionalLinkedList_MiscellaneousTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    // ================== roll back ==========================

    @Test
    public void rollbackPuts() throws InterruptedException {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.put("1");
        long version = stm.getTime();

        Transaction t = stm.startUpdateTransaction("rollback");
        setThreadLocalTransaction(t);
        deque.put("2");
        deque.put("3");
        t.abort();

        assertEquals(version, stm.getTime());
        assertEquals("[1]", deque.toString());
    }

    @Test
    public void rollbackTakes() throws InterruptedException {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.put("1");
        deque.put("2");
        deque.put("3");
        long version = stm.getTime();

        Transaction t = stm.startUpdateTransaction("rollback");
        setThreadLocalTransaction(t);
        deque.take();
        deque.take();
        t.abort();

        assertEquals(version, stm.getTime());
        assertEquals("[1, 2, 3]", deque.toString());
    }

    // ========================================


    @Test
    public void testToString() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        assertEquals("[]", deque.toString());

        deque.add("1");
        assertEquals("[1]", deque.toString());

        deque.add("2");
        deque.add("3");
        assertEquals("[1, 2, 3]", deque.toString());
    }
}

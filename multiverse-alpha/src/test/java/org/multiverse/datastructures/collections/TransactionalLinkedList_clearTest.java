package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_clearTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void clearEmptyDeque() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        long version = stm.getTime();

        deque.clear();
        assertEquals(version, stm.getTime());
        assertTrue(deque.isEmpty());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void clearNonEmptyDeque() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");
        deque.add("3");

        long version = stm.getTime();
        deque.clear();
        assertEquals(version + 1, stm.getTime());
        assertTrue(deque.isEmpty());
        assertEquals("[]", deque.toString());
    }
}

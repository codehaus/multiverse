package org.multiverse.datastructures.collections;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_removeObjectTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void remove_notFound() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");

        long version = stm.getTime();
        boolean result = deque.remove("3");
        assertFalse(result);
        assertEquals(version, stm.getTime());
        assertEquals(2, deque.size());
        assertEquals("[1, 2]", deque.toString());
    }

    @Test
    public void remove_fromEmptyList() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        boolean result = deque.remove("1");
        assertFalse(result);
        assertEquals(version, stm.getTime());
        assertEquals(0, deque.size());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void remove_fromSingletonList() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");

        long version = stm.getTime();
        boolean result = deque.remove("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals(0, deque.size());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void remove_head() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");

        long version = stm.getTime();
        boolean result = deque.remove("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals(1, deque.size());
        assertEquals("[2]", deque.toString());
    }

    @Test
    public void removeTail() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");

        long version = stm.getTime();
        boolean result = deque.remove("2");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals(1, deque.size());
        assertEquals("[1]", deque.toString());

    }

    @Test
    public void remove_betweenHeadAndTail() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");
        deque.add("3");
        deque.add("4");
        deque.add("5");

        long version = stm.getTime();
        boolean result = deque.remove("3");
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals(4, deque.size());
        assertEquals("[1, 2, 4, 5]", deque.toString());
    }
}

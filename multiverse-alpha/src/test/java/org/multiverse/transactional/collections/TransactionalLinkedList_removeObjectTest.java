package org.multiverse.transactional.collections;

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
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        boolean result = list.remove("3");
        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertEquals(2, list.size());
        assertEquals("[1, 2]", list.toString());
    }

    @Test
    public void remove_fromEmptyList() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();
        boolean result = list.remove("1");
        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void remove_fromSingletonList() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        long version = stm.getVersion();
        boolean result = list.remove("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(0, list.size());
        assertEquals("[]", list.toString());
    }

    @Test
    public void remove_head() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        boolean result = list.remove("1");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[2]", list.toString());
    }

    @Test
    public void removeTail() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        boolean result = list.remove("2");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[1]", list.toString());

    }

    @Test
    public void remove_betweenHeadAndTail() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");
        list.add("5");

        long version = stm.getVersion();
        boolean result = list.remove("3");
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(4, list.size());
        assertEquals("[1, 2, 4, 5]", list.toString());
    }
}

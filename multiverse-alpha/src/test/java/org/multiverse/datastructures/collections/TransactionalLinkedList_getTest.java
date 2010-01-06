package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_getTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    // ============ get(int) ================================

    @Test
    public void getTooSmallIndex() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        long version = stm.getTime();
        try {
            deque.get(-1);
            fail();
        } catch (IndexOutOfBoundsException ex) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void getTooLargeIndex() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");

        long version = stm.getTime();
        try {
            deque.get(1);
            fail();
        } catch (IndexOutOfBoundsException ex) {
        }
        assertEquals(version, stm.getTime());
        assertEquals("[1]", deque.toString());
    }

    @Test
    public void get() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");
        deque.add("3");

        long version = stm.getTime();
        assertEquals("1", deque.get(0));
        assertEquals("2", deque.get(1));
        assertEquals("3", deque.get(2));
        assertEquals(version, stm.getTime());
        assertEquals("[1, 2, 3]", deque.toString());
    }

}

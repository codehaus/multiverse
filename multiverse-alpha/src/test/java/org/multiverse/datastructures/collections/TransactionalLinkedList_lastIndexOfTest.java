package org.multiverse.datastructures.collections;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

import java.util.List;

public class TransactionalLinkedList_lastIndexOfTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void lastIndexOfNullFails() {
        List<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();
        try {
            deque.lastIndexOf(null);
            fail();
        } catch (NullPointerException ex) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[]", deque.toString());
    }

    @Test
    public void lastIndexOf() {
        List<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");
        deque.add("3");
        deque.add("1");

        long version = stm.getTime();
        assertEquals(3, deque.lastIndexOf("1"));
        assertEquals(1, deque.lastIndexOf("2"));
        assertEquals(2, deque.lastIndexOf("3"));
        assertEquals(-1, deque.lastIndexOf("a"));
        assertEquals(version, stm.getTime());
        assertEquals("[1, 2, 3, 1]", deque.toString());
    }
}

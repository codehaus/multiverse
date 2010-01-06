package org.multiverse.datastructures.collections;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.TestUtils.testIncomplete;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

import static java.util.Arrays.asList;
import java.util.LinkedList;
import java.util.List;

public class TransactionalLinkedList_addAllTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNullListNullPointerException() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("1");
        deque.add("2");

        long version = stm.getTime();
        try {
            deque.addAll(null);
            fail();
        } catch (NullPointerException ignore) {
        }

        assertEquals(version, stm.getTime());
        assertEquals("[1, 2]", deque.toString());
    }

    @Test
    public void test() {
        List<String> l = asList("1", "2", "3");

        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("a");
        deque.add("b");

        long version = stm.getTime();

        boolean result = deque.addAll(l);
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals("[a, b, 1, 2, 3]", deque.toString());
    }

    @Test
    public void whenListEmptyItemsAreAdded() {
        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();
        deque.add("a");
        deque.add("b");

        long version = stm.getTime();

        boolean result = deque.addAll(new LinkedList<String>());
        assertFalse(result);
        assertEquals(version, stm.getTime());
        assertEquals("[a, b]", deque.toString());
    }

    @Test
    public void whenListIsNonEmptyItemsAreAddedToEnd() {
        List<String> l = asList("1", "2", "3");

        TransactionalLinkedList<String> deque = new TransactionalLinkedList<String>();

        long version = stm.getTime();

        boolean result = deque.addAll(l);
        assertTrue(result);
        assertEquals(version + 1, stm.getTime());
        assertEquals("[1, 2, 3]", deque.toString());
    }

    @Test
    public void testAddAllWithIndex() {
        testIncomplete();
    }
}

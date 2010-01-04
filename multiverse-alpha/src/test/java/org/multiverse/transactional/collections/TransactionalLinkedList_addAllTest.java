package org.multiverse.transactional.collections;

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
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        try {
            list.addAll(null);
            fail();
        } catch (NullPointerException ignore) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[1, 2]", list.toString());
    }

    @Test
    public void test() {
        List<String> l = asList("1", "2", "3");

        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("a");
        list.add("b");

        long version = stm.getVersion();

        boolean result = list.addAll(l);
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[a, b, 1, 2, 3]", list.toString());
    }

    @Test
    public void whenListEmptyItemsAreAdded() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("a");
        list.add("b");

        long version = stm.getVersion();

        boolean result = list.addAll(new LinkedList<String>());
        assertFalse(result);
        assertEquals(version, stm.getVersion());
        assertEquals("[a, b]", list.toString());
    }

    @Test
    public void whenListIsNonEmptyItemsAreAddedToEnd() {
        List<String> l = asList("1", "2", "3");

        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();

        long version = stm.getVersion();

        boolean result = list.addAll(l);
        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1, 2, 3]", list.toString());
    }

    @Test
    public void testAddAllWithIndex() {
        testIncomplete();
    }
}

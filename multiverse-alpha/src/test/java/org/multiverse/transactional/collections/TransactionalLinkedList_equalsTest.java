package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalLinkedList_equalsTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test
    public void whenComparedWithDifferentListImplementation() {
        TransactionalLinkedList<String> list1 = new TransactionalLinkedList<String>();
        list1.add("1");
        list1.add("2");

        ArrayList<String> list2 = new ArrayList<String>();
        list2.add("1");
        list2.add("2");

        assertEquals(list1, list2);
        assertEquals(list2, list1);
    }

    @Test
    public void whenEquals() {
        TransactionalLinkedList<String> list1 = new TransactionalLinkedList<String>();
        list1.add("1");
        list1.add("2");

        TransactionalLinkedList<String> list2 = new TransactionalLinkedList<String>();
        list2.add("1");
        list2.add("2");

        assertEquals(list1, list2);
        assertEquals(list2, list1);
    }


    @Test
    public void whenSizeNonEqual_thenNotEqual() {
        TransactionalLinkedList<String> list1 = new TransactionalLinkedList<String>();
        list1.add("1");
        list1.add("2");

        TransactionalLinkedList<String> list2 = new TransactionalLinkedList<String>();
        list2.add("1");
        list2.add("2");
        list2.add("3");

        assertFalse(list1.equals(list2));
        assertFalse(list2.equals(list1));
    }

    @Test
    public void whenSizeEqualButContentDifferent_thenNotEqual() {
        TransactionalLinkedList<String> list1 = new TransactionalLinkedList<String>();
        list1.add("1");
        list1.add("2");

        TransactionalLinkedList<String> list2 = new TransactionalLinkedList<String>();
        list2.add("2");
        list2.add("1");

        assertFalse(list1.equals(list2));
        assertFalse(list2.equals(list1));
    }

    @Test
    public void listNeverEqualsNull() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        assertFalse(list.equals(null));

        list.add("1");
        list.add("2");

        assertFalse(list.equals(null));
    }

    @Test
    public void testEqualsThis() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        assertTrue(list.equals(list));

        list.add("foo");
        list.add("bar");

        assertTrue(list.equals(list));
    }
}

package org.multiverse.transactional.collections;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import org.multiverse.api.Stm;
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
    //@Ignore
    public void testNonEqualLists() {
        TransactionalLinkedList<String> list1 = new TransactionalLinkedList<String>();
        list1.add("1");
        list1.add("2");

        TransactionalLinkedList<String> list2 = new TransactionalLinkedList<String>();
        list2.add("3");


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

    //@Ignore
    @Test
    public void testEqualsThis() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        assertTrue(list.equals(list));

        list.add("foo");
        list.add("bar");

        assertTrue(list.equals(list));
    }
}

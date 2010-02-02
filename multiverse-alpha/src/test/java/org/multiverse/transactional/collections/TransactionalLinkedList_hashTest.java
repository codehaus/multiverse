package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalLinkedList_hashTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void equalListsGiveEqualHashCode() {
        TransactionalLinkedList<String> list1 = new TransactionalLinkedList<String>();
        list1.add("foo");

        TransactionalLinkedList<String> list2 = new TransactionalLinkedList<String>();
        list2.add("foo");

        assertEquals(list1.hashCode(), list2.hashCode());
    }
}

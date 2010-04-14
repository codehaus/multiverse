package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalArrayList_iteratorTest {

    @Before
    public void test() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenListEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        Iterator<String> it = list.iterator();
        assertFalse(it.hasNext());
    }

    @Test
    public void whenNotEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        Iterator<String> it = list.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
        assertFalse(it.hasNext());
    }
}

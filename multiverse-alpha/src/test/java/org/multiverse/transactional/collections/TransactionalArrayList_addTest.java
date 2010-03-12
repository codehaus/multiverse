package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransactionalArrayList_addTest {

    @Test
    public void whenEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        boolean result = list.add("foo");

        assertTrue(result);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0));
    }

    @Test
    public void whenNotEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("one");

        boolean result = list.add("bar");

        assertTrue(result);
        assertEquals(2, list.size());
        assertEquals("bar", list.get(1));
    }

    @Test
    public void whenCapacityExceeded() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>(3);

        list.add("one");
        list.add("two");
        list.add("three");
        list.add("four");

        assertEquals("[one, two, three, four]", list.toString());
    }

    @Test
    public void complexScenario() {
        TransactionalArrayList<Integer> list = new TransactionalArrayList<Integer>();

        for (int k = 0; k < 100000; k++) {
            list.add(k);
        }

        for (int k = 0; k < 100000; k++) {
            assertEquals(k, (int) list.get(k));
        }
    }
}

package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_addTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();

        boolean result = list.add("foo");

        assertEquals(version + 1, stm.getVersion());
        assertTrue(result);
        assertEquals(1, list.size());
        assertEquals("foo", list.get(0));
    }

    @Test
    public void whenNotEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("one");

        long version = stm.getVersion();
        boolean result = list.add("bar");

        assertEquals(version + 1, stm.getVersion());
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

        long version = stm.getVersion();
        list.add("four");

        assertEquals(version + 1, stm.getVersion());
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

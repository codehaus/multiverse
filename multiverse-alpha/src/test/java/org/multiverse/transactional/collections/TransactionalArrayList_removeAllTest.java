package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_removeAllTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenCollectionNull_thenNullPointerException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();

        try {
            list.removeAll(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCollectionEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        boolean changed = list.removeAll(new HashSet<String>());

        assertFalse(changed);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNoMatchingElements() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        Set<String> items = new HashSet<String>();
        items.add("d");
        items.add("e");

        long version = stm.getVersion();
        boolean changed = list.removeAll(items);

        assertFalse(changed);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenMatchingElements() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c", "d", "e");

        Set<String> items = new HashSet<String>();
        items.add("a");
        items.add("c");
        items.add("e");

        long version = stm.getVersion();
        boolean changed = list.removeAll(items);

        assertTrue(changed);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(2, list.size());
        assertEquals("[b, d]", list.toString());
    }

    @Test
    public void whenElementMatchingMultipleTimes() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "a", "d", "e");

        Set<String> items = new HashSet<String>();
        items.add("a");

        long version = stm.getVersion();
        boolean changed = list.removeAll(items);

        assertTrue(changed);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(3, list.size());
        assertEquals("[b, d, e]", list.toString());
    }
}

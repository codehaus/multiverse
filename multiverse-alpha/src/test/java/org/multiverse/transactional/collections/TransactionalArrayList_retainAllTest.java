package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_retainAllTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenCollectionNull_thenNullPointerException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        long version = stm.getVersion();

        try {
            list.retainAll(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[a, b, c]", list.toString());
    }

    @Test
    public void whenMatchingItems_othersRemoved() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c", "e", "a");

        long version = stm.getVersion();

        Set<String> set = new HashSet<String>();
        set.add("a");
        set.add("e");
        boolean changed = list.retainAll(set);

        assertTrue(changed);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[a, e, a]", list.toString());
    }

    @Test
    public void whenCollectionEmpty_thenEverythingRemoved() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        long version = stm.getVersion();

        boolean changed = list.retainAll(new HashSet<String>());

        assertTrue(changed);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenNothingMatches_thenEverythingRemoved() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        long version = stm.getVersion();

        HashSet<String> strings = new HashSet<String>();
        strings.add("x");
        strings.add("y");

        boolean changed = list.retainAll(strings);

        assertTrue(changed);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenEverythingMatches_thenNoChange() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c", "b", "b", "a");

        long version = stm.getVersion();

        HashSet<String> strings = new HashSet<String>();
        strings.add("a");
        strings.add("b");
        strings.add("c");
        boolean changed = list.retainAll(strings);

        assertFalse(changed);
        assertEquals(version, stm.getVersion());
        assertEquals("[a, b, c, b, b, a]", list.toString());
    }
}

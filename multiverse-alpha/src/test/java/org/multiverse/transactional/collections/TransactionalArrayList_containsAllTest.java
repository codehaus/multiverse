package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_containsAllTest {
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
            list.containsAll(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCollectionEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");

        long version = stm.getVersion();
        boolean result = list.containsAll(new LinkedList());

        assertTrue(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenAllElementMatch() {
        Set<String> set = new HashSet<String>();
        set.add("a");
        set.add("b");

        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");

        long version = stm.getVersion();
        boolean result = list.containsAll(set);

        assertTrue(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNotAllElementsMatch() {
        Set<String> set = new HashSet<String>();
        set.add("a");
        set.add("d");

        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");

        long version = stm.getVersion();
        boolean result = list.containsAll(set);

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNullElement_thenNoProblem() {
        List<String> set = new LinkedList<String>();
        set.add("a");
        set.add(null);

        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add(null);
        list.add("c");

        long version = stm.getVersion();
        boolean result = list.containsAll(set);

        assertTrue(result);
        assertEquals(version, stm.getVersion());
    }
}

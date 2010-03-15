package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_add2Test {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();

        try {
            list.add(-1, "a");
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[]", list.toString());
    }

    @Test
    public void whenIndexTooBig_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b");

        long version = stm.getVersion();

        try {
            list.add(3, "a");
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(version, stm.getVersion());
        assertEquals("[a, b]", list.toString());
    }

    @Test
    public void whenNullItemAdded() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();

        list.add(0, null);

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[null]", list.toString());
    }

    @Test
    public void whenItemAddedToEmptyList() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();

        list.add(0, "a");

        assertEquals(version + 1, stm.getVersion());
        assertEquals(1, list.size());
        assertEquals("[a]", list.toString());
    }

    @Test
    public void whenItemAddedToEnd() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        long version = stm.getVersion();

        list.add(3, "d");

        assertEquals(version + 1, stm.getVersion());
        assertEquals(4, list.size());
        assertEquals("[a, b, c, d]", list.toString());
    }

    @Test
    public void whenItemAddedInbetween() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "d", "e");

        long version = stm.getVersion();

        list.add(2, "c");

        assertEquals(version + 1, stm.getVersion());
        assertEquals(5, list.size());
        assertEquals("[a, b, c, d, e]", list.toString());
    }

    @Test
    public void whenItemAddedInFront() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("b", "c", "d");

        long version = stm.getVersion();

        list.add(0, "a");

        assertEquals(version + 1, stm.getVersion());
        assertEquals(4, list.size());
        assertEquals("[a, b, c, d]", list.toString());
    }
}

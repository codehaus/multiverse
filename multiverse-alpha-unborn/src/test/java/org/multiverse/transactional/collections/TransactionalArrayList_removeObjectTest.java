package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalArrayList_removeObjectTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenEmpty_returnFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        boolean changed = list.remove("foo");

        assertFalse(changed);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenItemNotFound_returnFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        long version = stm.getVersion();
        boolean changed = list.remove("foo");

        assertFalse(changed);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenObjectNull_thenRemovalAlsoSucces() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", null, "c");

        long version = stm.getVersion();
        boolean changed = list.remove(null);

        assertTrue(changed);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[a, c]", list.toString());
        assertEquals(2, list.size());
    }

    @Test
    public void whenObjectFound_thenRemoved() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        long version = stm.getVersion();
        boolean changed = list.remove("b");

        assertTrue(changed);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[a, c]", list.toString());
        assertEquals(2, list.size());
    }

    @Test
    public void whenMultipleOccurrences_thenOnlyFirstRemoved() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c", "b");

        long version = stm.getVersion();
        boolean changed = list.remove("b");

        assertTrue(changed);
        assertEquals(version + 1, stm.getVersion());
        assertEquals("[a, c, b]", list.toString());
        assertEquals(3, list.size());
    }
}

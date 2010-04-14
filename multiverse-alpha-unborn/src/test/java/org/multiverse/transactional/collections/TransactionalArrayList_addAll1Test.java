package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalArrayList_addAll1Test {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenCollectionNull_thenNullPointerException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();

        try {
            list.addAll(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenCollectionEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();

        boolean result = list.addAll(new LinkedList<String>());

        assertFalse(result);
        assertEquals("[]", list.toString());
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenMultipleItems() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");

        List<String> c = new LinkedList<String>();
        c.add("3");
        c.add("4");
        c.add("5");

        long version = stm.getVersion();
        boolean result = list.addAll(c);

        assertTrue(result);
        assertEquals(version + 1, stm.getVersion());
        assertEquals(5, list.size());
        assertEquals("[1, 2, 3, 4, 5]", list.toString());
    }
}

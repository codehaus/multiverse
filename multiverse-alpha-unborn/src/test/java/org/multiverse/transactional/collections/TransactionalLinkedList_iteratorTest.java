package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_iteratorTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test(expected = NoSuchElementException.class)
    public void whenListEmpty() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        Iterator<String> it = list.iterator();
        assertFalse(it.hasNext());
        it.next();
    }

    @Test
    public void whenMultipleItemsAvailable() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("4");

        Iterator<String> it = list.iterator();
        assertTrue(it.hasNext());
        assertEquals("1", it.next());

        assertTrue(it.hasNext());
        assertEquals("2", it.next());

        assertTrue(it.hasNext());
        assertEquals("3", it.next());

        assertTrue(it.hasNext());
        assertEquals("4", it.next());

        assertFalse(it.hasNext());
    }

    @Test
    public void whenItemRemovedFromIterator() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        Iterator<String> it = list.iterator();
        it.next();
        long version = stm.getVersion();
        it.remove();

        assertEquals(version + 1, stm.getVersion());
        assertEquals("[2]", list.toString());
    }

    @Test(expected = NoSuchElementException.class)
    public void removeWithoutNextCalled() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        Iterator<String> it = list.iterator();
        it.remove();
    }
}

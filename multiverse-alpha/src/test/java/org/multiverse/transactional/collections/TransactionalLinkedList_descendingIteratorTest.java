package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.Iterator;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.setThreadLocalTransaction;

public class TransactionalLinkedList_descendingIteratorTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        setThreadLocalTransaction(null);
    }

    @Test(expected = NoSuchElementException.class)
    public void emptyList() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        Iterator<String> it = list.descendingIterator();
        assertFalse(it.hasNext());
        it.next();
    }

    @Test
    public void nonEmptyList() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        Iterator<String> it = list.descendingIterator();
        assertTrue(it.hasNext());
        assertEquals("2", it.next());

        assertTrue(it.hasNext());
        assertEquals("1", it.next());

        assertFalse(it.hasNext());
    }

    @Test
    public void remove() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");
        list.add("2");

        Iterator<String> it = list.descendingIterator();
        it.next();
        long version = stm.getVersion();
        it.remove();

        assertEquals(version + 1, stm.getVersion());
        assertEquals("[1]", list.toString());
    }

    @Test(expected = NoSuchElementException.class)
    public void removeWithoutNextCalled() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        list.add("1");

        Iterator<String> it = list.descendingIterator();
        it.remove();
    }
}

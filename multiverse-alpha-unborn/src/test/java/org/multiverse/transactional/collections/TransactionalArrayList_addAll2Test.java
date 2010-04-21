package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalArrayList_addAll2Test {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenCollectionEmpty_thenReturnFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b");

        long version = stm.getVersion();
        boolean changed = list.addAll(0, new HashSet<String>());

        assertFalse(changed);
        assertEquals(version, stm.getVersion());
        assertEquals(2, list.size());
        assertEquals("[a, b]", list.toString());
    }

    @Test
    public void whenCollectionNull_thenNullPointerException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        try {
            list.addAll(0, null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        try {
            list.addAll(-1, new HashSet<String>());
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenIndexTooBig_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b");

        long version = stm.getVersion();
        try {
            list.addAll(3, new HashSet<String>());
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    @Ignore
    public void whenAddedInFront() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("c", "d");

        List<String> items = new LinkedList<String>();
        items.add("a");
        items.add("b");

        list.addAll(0, items);

        assertEquals("[a, b, c, d]", list.toString());
    }

    @Test
    @Ignore
    public void whenAddedToEnd() {

    }

    @Test
    @Ignore
    public void whenAddedInBetween() {

    }
}

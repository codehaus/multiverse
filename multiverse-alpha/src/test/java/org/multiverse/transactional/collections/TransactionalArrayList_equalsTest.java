package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_equalsTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenObjectNull_thenFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        boolean result = list.equals(null);

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenObjectSame_thenTrue() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        boolean result = list.equals(list);

        assertTrue(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenNotList_thenFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        boolean result = list.equals(new HashSet());

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenSizeDiffers_thenFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("foo");

        List that = new LinkedList();
        that.add("foo");
        that.add("bar");

        long version = stm.getVersion();
        boolean result = list.equals(that);

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenContentSame() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        List<String> that = new LinkedList<String>();
        that.add("1");
        that.add("2");
        that.add("3");

        long version = stm.getVersion();
        boolean result = list.equals(that);

        assertTrue(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenContentSameButOrderDifferent() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        List<String> that = new LinkedList<String>();
        that.add("2");
        that.add("3");
        that.add("1");

        long version = stm.getVersion();
        boolean result = list.equals(that);

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenContentDiffers() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        List<String> that = new LinkedList<String>();
        that.add("1");
        that.add("2");
        that.add("4");

        long version = stm.getVersion();
        boolean result = list.equals(that);

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }
}

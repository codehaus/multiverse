package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalArrayList_set2Test {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        try {
            list.set(-1, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {

        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenIndexTooBig_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");

        long version = stm.getVersion();
        try {
            list.set(3, "foo");
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenItemWasNull_replaceSuccess() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add(null);
        list.add("c");

        long version = stm.getVersion();
        String returned = list.set(1, "b");

        assertEquals(returned, null);
        assertEquals("b", list.get(1));
        assertEquals(version + 1, stm.getVersion());
    }

    @Test
    public void whenItemReplacedByNull_replaceSuccess() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");

        long version = stm.getVersion();
        String returned = list.set(1, null);

        assertEquals(returned, "b");
        assertEquals(null, list.get(1));
        assertEquals(version + 1, stm.getVersion());
    }

    @Test
    public void whenItemReplaced() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add("b");
        list.add("c");

        long version = stm.getVersion();
        String returned = list.set(1, "newb");

        assertEquals(returned, "b");
        assertEquals("newb", list.get(1));
        assertEquals(version + 1, stm.getVersion());
    }
}

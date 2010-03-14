package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_removeIndexTest {
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
            list.remove(-1);
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

        long version = stm.getVersion();

        try {
            list.remove(2);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenIndexVeryMuchTooBig_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add("b");

        long version = stm.getVersion();

        try {
            list.remove(20);
            fail();
        } catch (IndexOutOfBoundsException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    @Ignore
    public void when() {

    }
}

package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_clearTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        list.clear();

        assertEquals(version, stm.getVersion());
        assertEquals(0, list.size());
    }

    @Test
    public void whenNotEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("0");
        list.add("1");
        list.add("2");

        long version = stm.getVersion();
        list.clear();

        assertEquals(version + 1, stm.getVersion());
        assertEquals(0, list.size());
    }
}

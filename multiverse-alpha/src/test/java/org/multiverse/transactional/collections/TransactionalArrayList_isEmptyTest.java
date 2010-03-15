package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_isEmptyTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenNonEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add("b");

        long version = stm.getVersion();
        boolean result = list.isEmpty();

        assertFalse(result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        boolean result = list.isEmpty();

        assertTrue(result);
        assertEquals(version, stm.getVersion());
    }
}

package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_toArrayTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        Object[] result = list.toArray();
        assertNotNull(result);
        assertEquals(0, result.length);
    }

    @Test
    public void whenNonEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        long version = stm.getVersion();

        Object[] result = list.toArray();

        assertEquals(version, stm.getVersion());
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }

}

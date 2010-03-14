package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_removeObjectTest {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenEmpty_returnFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        boolean changed = list.remove("foo");

        assertFalse(changed);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenItemNotFound_returnFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        long version = stm.getVersion();
        boolean changed = list.remove("foo");

        assertFalse(changed);
        assertEquals(version, stm.getVersion());
    }

    @Test
    @Ignore
    public void whenObjectNull_thenRemovalAlsoSucces() {

    }

    @Test
    @Ignore
    public void whenObjectFound_thenRemoved() {

    }

    @Test
    @Ignore
    public void whenMultipleOccurrences_thenOnlyFirstRemoved() {

    }
}

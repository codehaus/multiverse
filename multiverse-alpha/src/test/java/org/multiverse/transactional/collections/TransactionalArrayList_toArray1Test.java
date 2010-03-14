package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;

public class TransactionalArrayList_toArray1Test {
    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
    }

    @Test
    public void whenArrayIsBigger_thenItemsFollowingAreCleared() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        String[] in = new String[]{"1", "2", "3", "4", "5"};
        String[] out = list.toArray(in);

        assertSame(in, out);
        assertEquals("a", out[0]);
        assertEquals("b", out[1]);
        assertEquals("c", out[2]);
        assertNull(out[3]);
        assertNull(out[4]);
    }

    @Test
    public void whenArrayNull_thenNullPointerException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();

        long version = stm.getVersion();
        try {
            list.toArray(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenArrayBigEnough() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        String[] in = new String[3];
        String[] out = list.toArray(in);

        assertSame(in, out);
        assertEquals("a", out[0]);
        assertEquals("b", out[1]);
        assertEquals("c", out[2]);
    }

    @Test
    public void whenArrayNotBigEnough() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>("a", "b", "c");

        String[] in = new String[2];
        String[] out = list.toArray(in);

        assertFalse(in == out);
        assertEquals(3, out.length);
        assertEquals("a", out[0]);
        assertEquals("b", out[1]);
        assertEquals("c", out[2]);
    }

}

package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Stm;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalArrayList_indexOfTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTransaction();
    }

    @Test
    public void whenElementNotAvailble_thenMinesOneReturned() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        long version = stm.getVersion();
        int result = list.indexOf("4");

        assertEquals(version, stm.getVersion());
        assertEquals(-1, result);
    }

    @Test
    public void whenElementNull() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add(null);
        list.add("4");

        long version = stm.getVersion();
        int result = list.indexOf(null);

        assertEquals(2, result);
        assertEquals(version, stm.getVersion());
    }

    @Test
    public void whenMultipleOccurrencesFirstElementReturned() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("2");
        list.add("5");

        long version = stm.getVersion();
        int result = list.indexOf("2");
        assertEquals(1, result);
        assertEquals(version, stm.getVersion());
    }
}

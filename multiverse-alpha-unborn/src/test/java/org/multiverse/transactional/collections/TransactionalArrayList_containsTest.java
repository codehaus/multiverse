package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalArrayList_containsTest {

    @Before
    public void setUp(){
           clearThreadLocalTransaction();
    }

    @Test
    public void whenElementNotAvailable_thenFalse() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        boolean result = list.contains("4");
        assertFalse(result);
    }

    @Test
    public void whenElementNull() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add(null);
        list.add("4");

        boolean result = list.contains(null);
        assertTrue(result);
    }
}

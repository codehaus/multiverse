package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionalArrayList_containsTest {

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

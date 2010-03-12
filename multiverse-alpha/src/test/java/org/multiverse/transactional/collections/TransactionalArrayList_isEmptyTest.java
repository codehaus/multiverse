package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransactionalArrayList_isEmptyTest {

    @Test
    public void whenNonEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("a");
        list.add("b");

        boolean result = list.isEmpty();
        assertFalse(result);
    }

    @Test
    public void whenEmpty() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        boolean result = list.isEmpty();
        assertTrue(result);
    }
}

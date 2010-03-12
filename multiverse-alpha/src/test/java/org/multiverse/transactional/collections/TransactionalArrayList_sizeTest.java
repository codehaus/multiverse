package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransactionalArrayList_sizeTest {

    @Test
    public void whenEmpty() {
        TransactionalArrayList<String> array = new TransactionalArrayList<String>();
        assertEquals(0, array.size());
    }
}

package org.multiverse.transactional.collections;

import static org.junit.Assert.assertFalse;
import org.junit.Test;
import static org.multiverse.TestUtils.testIncomplete;

public class TransactionalLinkedList_hashTest {

    @Test
    public void equalsWithNonCollection() {
        TransactionalLinkedList<String> list = new TransactionalLinkedList<String>();
        boolean equals = list.equals("foo");
        assertFalse(equals);
    }

    @Test
    public void hash() {
        testIncomplete();
    }
}

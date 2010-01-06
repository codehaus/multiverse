package org.multiverse.datastructures.collections;

import static org.junit.Assert.assertFalse;
import org.junit.Test;
import static org.multiverse.TestUtils.testIncomplete;

import java.util.concurrent.BlockingDeque;

public class TransactionalLinkedList_hashTest {

    @Test
    public void equalsWithNonCollection() {
        BlockingDeque<String> deque = new TransactionalLinkedList<String>();
        boolean equals = deque.equals("foo");
        assertFalse(equals);
    }

    @Test
    public void hash() {
        testIncomplete();
    }

}

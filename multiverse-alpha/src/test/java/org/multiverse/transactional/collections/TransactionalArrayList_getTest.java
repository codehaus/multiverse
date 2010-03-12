package org.multiverse.transactional.collections;

import org.junit.Test;

public class TransactionalArrayList_getTest {

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooSmall_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.get(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void whenIndexTooLarge_thenIndexOutOfBoundsException() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("foo");
        list.add("bar");

        list.get(3);
    }
}

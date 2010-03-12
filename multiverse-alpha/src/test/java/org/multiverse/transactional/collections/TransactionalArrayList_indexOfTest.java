package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransactionalArrayList_indexOfTest {

    @Test
    public void whenElementNotAvailble_thenMinesOneReturned() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");

        int result = list.indexOf("4");
        assertEquals(-1, result);
    }

    @Test
    public void whenElementNull() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add(null);
        list.add("4");

        int result = list.indexOf(null);
        assertEquals(2, result);
    }

    @Test
    public void whenMultipleOccurrencesFirstElementReturned() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("1");
        list.add("2");
        list.add("3");
        list.add("2");
        list.add("5");

        int result = list.indexOf("2");
        assertEquals(1, result);
    }
}

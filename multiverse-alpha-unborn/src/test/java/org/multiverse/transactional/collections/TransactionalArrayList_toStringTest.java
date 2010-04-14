package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

public class TransactionalArrayList_toStringTest {

    @Before
    public void setUp(){
        clearThreadLocalTransaction();
    }

    @Test
    public void whenEmptyList() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        String result = list.toString();
        assertEquals("[]", result);
    }

    @Test
    public void whenSomeElements() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add("zero");
        list.add("one");
        list.add("two");

        String result = list.toString();
        assertEquals("[zero, one, two]", result);
    }

    @Test
    public void whenNullElement() {
        TransactionalArrayList<String> list = new TransactionalArrayList<String>();
        list.add(null);

        String result = list.toString();
        assertEquals("[null]", result);
    }
}

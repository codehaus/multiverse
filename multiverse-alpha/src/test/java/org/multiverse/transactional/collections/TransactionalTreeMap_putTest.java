package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_putTest {

    @Test
    public void whenKeyNull_thenNullPointerException() {
        TransactionalTreeMap map = new TransactionalTreeMap();

        try {
            map.put(null, "foo");
            fail();
        } catch (NullPointerException expected) {
        }

        assertEquals(0, map.size());
    }

    @Test
    public void whenTreeIsEmpty() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();

        String result = map.put("foo", "bar");
        assertNull(result);
        assertEquals(1, map.size());
    }

    public void whenTreeNonEmpty() {

    }

    public void whenKeyExists() {

    }
}

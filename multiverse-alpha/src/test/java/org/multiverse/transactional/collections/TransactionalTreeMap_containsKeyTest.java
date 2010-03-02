package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_containsKeyTest {

    @Test(expected = NullPointerException.class)
    public void whenKeyNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.containsKey(null);
    }

    @Test
    public void whenKeyNotFound() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put("1", "1");
        map.put("2", "2");
        map.put("3", "3");

        assertFalse(map.containsKey("4"));
    }

    @Test
    public void whenKeyFound() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put("1", "1");
        map.put("2", "2");
        map.put("3", "3");

        assertTrue(map.containsKey("2"));
    }
}

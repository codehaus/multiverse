package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_clearTest {

    @Test
    public void whenMapEmpty_thenNothingHappens() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void whenSingletonMap_thenCleared() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put("foo", "bar");
        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    @Ignore
    public void whenMapContainsLotsOfStuff_thenCleared() {

    }
}

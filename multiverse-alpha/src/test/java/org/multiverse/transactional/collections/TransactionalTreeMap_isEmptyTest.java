package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMap;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_isEmptyTest {

    @Test
    public void whenNoItems_thenTrue() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        assertTrue(map.isEmpty());
    }

    @Test
    public void whenSingleton_thenFalse() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put("foo", "bar");

        assertFalse(map.isEmpty());
    }

    @Test
    public void whenMultipleItems_thenFalse() {
        TransactionalTreeMap<String, String> map = createTreeMap(100);
        assertFalse(map.isEmpty());
    }
}

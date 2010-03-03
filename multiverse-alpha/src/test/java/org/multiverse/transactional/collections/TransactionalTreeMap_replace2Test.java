package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMapExcluding;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMapIncluding;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_replace2Test {

    @Test(expected = NullPointerException.class)
    public void whenKeyNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.replace(null, "key");
    }

    @Test(expected = NullPointerException.class)
    public void whenValueNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.replace("key", null);
    }

    @Test
    public void whenKeyNotPresent() {
        TransactionalTreeMap<String, String> map = createTreeMapExcluding(1000, "key");

        String old = map.replace("key", "newvalue");
        assertNull(old);
    }

    @Test
    public void whenKeyPresent() {
        TransactionalTreeMap<String, String> map = createTreeMapIncluding(1000, "key", "oldvalue");

        String old = map.replace("key", "newvalue");
        assertEquals("oldvalue", old);
        assertEquals("newvalue", map.get("key"));
        assertEquals(1000, map.size());
    }

    @Test
    public void whenNoValueChange() {
        TransactionalTreeMap<String, String> map = createTreeMapIncluding(1000, "key", "value");

        String old = map.replace("key", "value");
        assertEquals("value", old);
        assertEquals("value", map.get("key"));
        assertEquals(1000, map.size());
    }
}

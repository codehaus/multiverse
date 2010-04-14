package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMapExcluding;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMapIncluding;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_putTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

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

    @Test
    public void whenTreeNonEmptyAndItemMissing() {
        TransactionalTreeMap<String, String> map = createTreeMapExcluding(100, "1");

        String old = map.put("1", "one");
        assertNull(old);
        assertEquals(101, map.size());
        assertEquals("one", map.get("1"));
    }

    @Test
    public void whenKeyExists() {
        TransactionalTreeMap<String, String> map = createTreeMapIncluding(100, "1", "one");

        String old = map.put("1", "newone");
        assertEquals("one", old);
        assertEquals(100, map.size());
        assertEquals("newone", map.get("1"));
    }
}

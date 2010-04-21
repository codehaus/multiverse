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

    @Test
    public void shouldLeftRotateWhenRightHeavyTree() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");

        assertTrue(map.height() == 2);
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(2));
        assertTrue(map.containsKey(3));
    }

    @Test
    public void shouldLeftRotateWhenRightHeavyTree2() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");
        map.put(5, "five");
        map.put(6, "six");

        assertTrue(map.height() == 4);
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(2));
        assertTrue(map.containsKey(3));
        assertTrue(map.containsKey(4));
        assertTrue(map.containsKey(5));
        assertTrue(map.containsKey(6));
    }

    @Test
    public void shouldDoubleLeftRotateWhenRightHeavyTreeWithLeftHeavyRightSubtree() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(1, "one");
        map.put(3, "three");
        map.put(2, "two");

        assertTrue(map.height() == 2);
    }

    @Test
    public void shouldRightRotateWhenLeftHeavyTree() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(3, "three");
        map.put(2, "two");
        map.put(1, "one");

        assertTrue(map.height() == 2);
    }

    @Test
    public void shouldRightRotateWhenLeftHeavyTree2() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(6, "six");
        map.put(5, "five");
        map.put(4, "four");
        map.put(3, "three");
        map.put(2, "two");
        map.put(1, "one");

        assertTrue(map.height() == 4);
        assertTrue(map.containsKey(1));
        assertTrue(map.containsKey(2));
        assertTrue(map.containsKey(3));
        assertTrue(map.containsKey(4));
        assertTrue(map.containsKey(5));
        assertTrue(map.containsKey(6));
        assertTrue(map.findNode(5).getParent() == null);
    }

    @Test
    public void shouldDoubleRightRotateWhenLeftHeavyTreeWithRightHeavyLeftSubtree() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(3, "three");
        map.put(1, "one");
        map.put(2, "two");

        assertTrue(map.height() == 2);
    }
}

package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.Random;

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
    public void shouldBalanceARightHeavyTree() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        map.put(4, "four");
        map.put(5, "five");
        map.put(6, "six");
        map.put(7, "seven");
        map.put(8, "eight");
        map.put(9, "nine");

        assertEquals(9, map.size());
        assertEquals(4, map.height());
        assertTrue("not balanced", map.isBalanced());
    }

    @Test
    public void shouldBalanceARightHeavyTreeWithLeftHeavyRightSubtree() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(1, "one");
        map.put(3, "three");
        map.put(2, "two");

        assertEquals(3, map.size());
        assertEquals(2, map.height());
        assertTrue("not balanced", map.isBalanced());
    }

    @Test
    public void shouldBalanceALeftHeavyTree() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(9, "nine");
        map.put(8, "eight");
        map.put(7, "seven");
        map.put(6, "six");
        map.put(5, "five");
        map.put(4, "four");
        map.put(3, "three");
        map.put(2, "two");
        map.put(1, "one");

        assertEquals(9, map.size());
        assertEquals(4, map.height());
        assertTrue("not balanced", map.isBalanced());
    }

    @Test
    public void shouldBalanceLeftHeavyTreeWithRightHeavyLeftSubtree() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        map.put(3, "three");
        map.put(1, "one");
        map.put(2, "two");

        assertEquals(3, map.size());
        assertTrue(map.height() == 2);
    }

    @Test
    public void shouldBalanceThousandRandomNumberInsertions() {
        TransactionalTreeMap<Integer, String> map = new TransactionalTreeMap<Integer, String>();
        int uniqueInsertionCount = 0;

        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            Integer randomNumber = new Integer(random.nextInt(10000));
            String oldValue = map.put(randomNumber, randomNumber.toString());
            if (oldValue == null) {
                uniqueInsertionCount++;
            }
        }

        assertEquals(uniqueInsertionCount, map.size());
        assertTrue("not balanced", map.isBalanced());
    }

}

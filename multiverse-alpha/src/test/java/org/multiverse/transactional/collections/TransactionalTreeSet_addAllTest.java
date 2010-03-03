package org.multiverse.transactional.collections;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_addAllTest {

    @Test
    public void whenSetEmpty_allItemsAdded() {
        Set<String> c = new HashSet<String>();
        c.add("a");
        c.add("b");
        c.add("c");

        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.addAll(c);

        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("a"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void whenCollectionEmpty_thenNoChange() {
        Set<String> c = new HashSet<String>();

        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("a");
        set.add("b");
        set.add("c");

        set.addAll(c);

        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("a"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void whenNoDuplicateItems_allAdded() {
        Set<String> c = new HashSet<String>();
        c.add("a");
        c.add("b");

        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("c");
        set.add("d");

        set.addAll(c);

        assertEquals(4, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("a"));
        assertTrue(set.contains("c"));
        assertTrue(set.contains("d"));
    }

    @Test
    public void whenSomeDuplicateItems_thoseAreNotAdded() {
        Set<String> c = new HashSet<String>();
        c.add("a");
        c.add("b");
        c.add("c");

        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("c");
        set.add("d");
        set.add("e");

        set.addAll(c);

        assertEquals(5, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
        assertTrue(set.contains("d"));
        assertTrue(set.contains("e"));
    }

    @Test
    public void whenAllDuplicates_thenNoChange() {
        Set<String> c = new HashSet<String>();
        c.add("a");
        c.add("b");
        c.add("c");

        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("a");
        set.add("b");
        set.add("c");

        set.addAll(c);

        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test(expected = NullPointerException.class)
    public void whenCollectionNull_thenNullPointerException() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.addAll(null);
    }
}

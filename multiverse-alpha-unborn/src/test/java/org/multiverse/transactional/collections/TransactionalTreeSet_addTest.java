package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_addTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenItemNull_thenNullPointerException() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add(null);
    }

    @Test
    public void whenSetEmpty() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();

        String item = "foo";
        boolean result = set.add(item);
        assertTrue(result);
        assertTrue(set.contains("foo"));
        assertEquals(1, set.size());
    }

    @Test
    public void whenItemNotAddedBefore() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("a");
        set.add("b");

        boolean result = set.add("c");
        assertTrue(result);
        assertEquals(3, set.size());
        assertTrue(set.contains("c"));
    }

    @Test
    public void whenItemAddedBefore() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("a");
        set.add("b");
        set.add("c");

        boolean result = set.add("b");
        assertFalse(result);
        assertEquals(3, set.size());
        assertTrue(set.contains("b"));
    }
}

package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_containsTest {

    @Test(expected = NullPointerException.class)
    public void whenNullArgument_thenNullPointerException() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.contains(null);
    }

    @Test
    public void whenSetEmpty_thenReturnFalse() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();

        boolean result = set.contains("foo");
        assertFalse(result);
    }

    @Test
    public void whenItemNotFound_thenReturnFalse() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("1");
        set.add("2");
        set.add("3");

        boolean result = set.contains("a");
        assertFalse(result);
    }

    @Test
    public void whenItemFound_thenReturnTrue() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("1");
        set.add("2");
        set.add("3");

        boolean result = set.contains("2");
        assertTrue(result);
    }
}

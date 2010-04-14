package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_containsAllTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenProvidedCollectionIsNull_thenNullPointerException() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.containsAll(null);
    }

    @Test
    public void whenProvidedCollectionIsEmpty_thenTrue() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();

        Set<String> provided = new HashSet<String>();
        boolean result = set.containsAll(provided);
        assertTrue(result);
    }

    @Test
    public void whenNotAllItemsFound_thenFalse() {
        Set<String> provided = new HashSet<String>();
        provided.add("a");
        provided.add("b");

        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("a");
        set.add("c");

        boolean result = set.containsAll(provided);
        assertFalse(result);
    }

    @Test
    public void whenAllItemsAvailable() {
        Set<String> provided = new HashSet<String>();
        provided.add("a");
        provided.add("b");

        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("a");
        set.add("b");
        set.add("c");

        boolean result = set.containsAll(provided);
        assertTrue(result);
    }
}

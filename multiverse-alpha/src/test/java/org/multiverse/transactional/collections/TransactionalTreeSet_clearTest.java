package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeSet;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_clearTest {

    @Test
    public void whenEmpty_thenNothingHappens() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.clear();

        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    @Test
    public void whenSingletonSet_thenCleared() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.add("foo");
        set.clear();

        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    @Test
    public void whenLotsOfItems() {
        TransactionalTreeSet<String> set = createTreeSet(100);

        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }
}

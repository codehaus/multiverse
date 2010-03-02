package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeSet;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_isEmptyTest {

    @Test
    public void whenEmpty() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        assertTrue(set.isEmpty());
    }

    @Test
    public void whenNotEmpty() {
        TransactionalTreeSet<String> set = createTreeSet(100);
        assertFalse(set.isEmpty());
    }
}

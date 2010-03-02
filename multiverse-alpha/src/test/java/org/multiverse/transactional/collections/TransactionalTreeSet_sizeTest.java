package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeSet;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_sizeTest {

    @Test
    public void whenEmpty() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        assertEquals(0, set.size());
    }

    @Test
    public void whenNonEmpty() {
        TransactionalTreeSet<String> set = createTreeSet(100);
        assertEquals(100, set.size());
    }
}

package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_addAllTest {

    @Test
    @Ignore
    public void whenSetEmpty_allItemsAdded() {

    }

    @Test
    @Ignore
    public void whenCollectionEmpty_thenNoChange() {
    }

    @Test
    @Ignore
    public void whenNoDuplicateItems_allAdded() {
    }

    @Test
    @Ignore
    public void whenSomeDuplicateItems_thoseAreNotAdded() {
    }

    @Test
    @Ignore
    public void whenAllDuplicates_thenNoChange() {

    }

    @Test(expected = NullPointerException.class)
    public void whenCollectionNull_thenNullPointerException() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.addAll(null);
    }
}

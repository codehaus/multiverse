package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeSet;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_isEmptyTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

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

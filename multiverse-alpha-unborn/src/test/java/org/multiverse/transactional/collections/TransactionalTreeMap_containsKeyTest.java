package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMapExcluding;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMapIncluding;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_containsKeyTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }


    @Test(expected = NullPointerException.class)
    public void whenKeyNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.containsKey(null);
    }

    @Test
    public void whenKeyNotFound() {
        TransactionalTreeMap<String, String> map = createTreeMapExcluding(1000, "50");
        assertFalse(map.containsKey("50"));
    }

    @Test
    public void whenKeyFound() {
        TransactionalTreeMap<String, String> map = createTreeMapIncluding(1000, "50", "somevalue");
        assertTrue(map.containsKey("50"));
    }
}

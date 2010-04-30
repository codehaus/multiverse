package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_entrySet_isEmptyTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenEmpty() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set entrySet = map.entrySet();

        assertTrue(entrySet.isEmpty());
    }

    @Test
    public void whenNonEmpty() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        map.put("1", "a");
        map.put("2", "b");
        map.put("3", "c");

        Set entrySet = map.entrySet();

        assertFalse(entrySet.isEmpty());
    }
}

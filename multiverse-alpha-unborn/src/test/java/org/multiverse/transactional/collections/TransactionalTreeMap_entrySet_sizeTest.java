package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_entrySet_sizeTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenEmpty() {
        TransactionalTreeMap map = new TransactionalTreeMap();

        Set entrySet = map.entrySet();
        assertEquals(0, entrySet.size());
    }

    @Test
    public void whenNotEmpty() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        map.put("1", "a");
        map.put("2", "b");
        map.put("3", "c");

        Set entrySet = map.entrySet();
        assertEquals(3, entrySet.size());
    }
}

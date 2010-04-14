package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMap;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_clearTest {

    @Before
    public void setUp(){
           clearThreadLocalTransaction();
    }
    
    @Test
    public void whenMapEmpty_thenNothingHappens() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void whenSingletonMap_thenCleared() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put("foo", "bar");
        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void whenMapContainsLotsOfStuff_thenCleared() {
        TransactionalTreeMap<String, String> map = createTreeMap(100);
        map.clear();

        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }
}

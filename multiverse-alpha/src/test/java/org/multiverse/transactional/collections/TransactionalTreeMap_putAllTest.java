package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMap;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_putAllTest {

    @Test
    @Ignore
    public void test() {
    }


    @Test
    public void whenProvidedMapEmpty_thenNothingHappens() {
        TransactionalTreeMap<String, String> map = createTreeMap(100);

        map.putAll(new HashMap<String, String>());

        assertEquals(100, map.size());
    }

    @Test(expected = NullPointerException.class)
    public void whenProvidedMapNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.putAll(null);
    }
}

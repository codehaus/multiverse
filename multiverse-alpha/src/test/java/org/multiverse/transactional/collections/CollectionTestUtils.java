package org.multiverse.transactional.collections;

import org.multiverse.TestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class CollectionTestUtils {

    public static TransactionalTreeMap<String, String> createTreeMap(int size) {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();

        Set<String> items = new HashSet<String>();
        for (int k = 0; k < size; k++) {
            String item;
            do {
                item = String.valueOf(TestUtils.randomInt(size * 10));
            } while (!items.add(item));

            map.put(item, item);
        }

        assertEquals(size, map.size());
        return map;
    }


    public static TransactionalTreeSet<String> createTreeSet(int size) {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();

        Set<String> items = new HashSet<String>();
        for (int k = 0; k < size; k++) {
            String item;
            do {
                item = String.valueOf(TestUtils.randomInt(size * 10));
            } while (!items.add(item));

            set.add(item);
        }

        assertEquals(size, set.size());
        return set;
    }

}

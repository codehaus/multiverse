package org.multiverse.transactional.collections;

import org.multiverse.TestUtils;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class CollectionTestUtils {

    public static TransactionalTreeMap<String, String> createTreeMapExcluding(int size, String key) {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();

        Set<String> done = new HashSet<String>();
        done.add(key);

        for (int k = 0; k < size; k++) {
            String item;
            do {
                item = String.valueOf(TestUtils.randomInt(size * 10));
            } while (!done.add(item));

            map.put(item, item);
        }

        assertEquals(size, map.size());
        return map;
    }

    public static TransactionalTreeMap<String, String> createTreeMapIncluding(int size, String key, String value) {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put(key, value);

        Set<String> done = new HashSet<String>();
        done.add(key);

        for (int k = 1; k < size; k++) {
            String item;
            do {
                item = String.valueOf(TestUtils.randomInt(size * 10));
            } while (!done.add(item));

            map.put(item, item);
        }

        assertEquals(size, map.size());
        return map;
    }

    public static TransactionalTreeMap<String, String> createTreeMap(int size) {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();

        Set<String> done = new HashSet<String>();

        for (int k = 0; k < size; k++) {
            String item;
            do {
                item = String.valueOf(TestUtils.randomInt(size * 10));
            } while (!done.add(item));

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

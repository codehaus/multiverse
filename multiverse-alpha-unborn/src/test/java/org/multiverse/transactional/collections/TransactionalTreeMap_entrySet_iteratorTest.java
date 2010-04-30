package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_entrySet_iteratorTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenEmpty() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set entrySet = map.entrySet();
        Iterator it = entrySet.iterator();
        assertNotNull(it);
        assertFalse(it.hasNext());
    }

    @Test
    @Ignore
    public void whenNonEmpty() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        map.put("1", "a");
        map.put("2", "b");
        map.put("3", "c");

        Set entrySet = map.entrySet();
        Iterator it = entrySet.iterator();

        assertNotNull(it);
        assertEqualEntry((Map.Entry) it.next(), "1", "a");
        assertEqualEntry((Map.Entry) it.next(), "2", "b");
        assertEqualEntry((Map.Entry) it.next(), "3", "c");

        assertFalse(it.hasNext());
    }

    public void assertEqualEntry(Map.Entry entry, Object key, Object value) {
        assertNotNull(entry);
        assertEquals(entry.getKey(), key);
        assertEquals(entry.getValue(), value);
    }
}

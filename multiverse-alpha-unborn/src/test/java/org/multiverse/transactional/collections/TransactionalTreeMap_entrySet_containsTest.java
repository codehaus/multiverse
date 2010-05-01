package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_entrySet_containsTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    public void whenNullArgument_thenFalse() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set<Map.Entry> entries = map.entrySet();
        boolean result = entries.contains(null);
        assertFalse(result);
    }

    @Test
    public void whenNonEntry_thenFalse() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set<Map.Entry> entries = map.entrySet();
        boolean result = entries.contains("foo");
        assertFalse(result);
    }

    @Test
    public void whenNotFound() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        map.put("1", "a");
        map.put("2", "b");
        map.put("3", "c");

        Set<Map.Entry> entries = map.entrySet();

        Map.Entry entry = mock(Map.Entry.class);
        when(entry.getKey()).thenReturn("4");

        boolean result = entries.contains(entry);
        assertFalse(result);
    }

    @Test
    public void whenFound() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        map.put("1", "a");
        map.put("2", "b");
        map.put("3", "c");

        Set<Map.Entry> entries = map.entrySet();

        Map.Entry entry = mock(Map.Entry.class);
        when(entry.getKey()).thenReturn("2");
        when(entry.getValue()).thenReturn("b");

        boolean result = entries.contains(entry);
        assertTrue(result);
    }
}

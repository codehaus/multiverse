package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
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
public class TransactionalTreeMap_entrySet_containsAllTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenNullArgument_thenNullPointerException() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set<Map.Entry> entries = map.entrySet();
        entries.containsAll(null);
    }

    @Test
    public void whenEmptyCollection_thenTrue() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set<Map.Entry> entries = map.entrySet();
        boolean result = entries.containsAll(new LinkedList());
        assertTrue(result);
    }

    @Test
    public void whenNotAllItemsContained() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        map.put("1", "a");
        map.put("2", "b");
        map.put("3", "c");

        Set<Map.Entry> entries = map.entrySet();

        Map.Entry item1 = mock(Map.Entry.class);
        when(item1.getKey()).thenReturn("1");
        when(item1.getValue()).thenReturn("a");

        Map.Entry item2 = mock(Map.Entry.class);
        when(item2.getKey()).thenReturn("2");
        when(item2.getValue()).thenReturn("xxxx");

        List items = new LinkedList();
        items.add(item1);
        items.add(item2);

        boolean result = entries.containsAll(items);
        assertFalse(result);
    }

    @Test
    public void whenAllItemsContained() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        map.put("1", "a");
        map.put("2", "b");

        Set<Map.Entry> entries = map.entrySet();

        Map.Entry item1 = mock(Map.Entry.class);
        when(item1.getKey()).thenReturn("1");
        when(item1.getValue()).thenReturn("a");

        Map.Entry item2 = mock(Map.Entry.class);
        when(item2.getKey()).thenReturn("2");
        when(item2.getValue()).thenReturn("b");

        List items = new LinkedList();
        items.add(item1);
        items.add(item2);

        boolean result = entries.containsAll(items);
        assertTrue(result);
    }
}

package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMap;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_putAllTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenProvidedMapNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.putAll(null);
    }

    @Test
    public void whenProvidedMapEmpty_thenNothingHappens() {
        TransactionalTreeMap<String, String> map = createTreeMap(100);
        map.putAll(new HashMap<String, String>());
        assertEquals(100, map.size());
    }

    @Test
    public void whenMapIsEmpty_allProvidedItemsAreAdded() {
        Map<String, String> provided = new HashMap<String, String>();
        provided.put("1", "one");
        provided.put("2", "two");
        provided.put("3", "three");

        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.putAll(provided);

        assertEquals(3, map.size());
        assertEquals("one", map.get("1"));
        assertEquals("two", map.get("2"));
        assertEquals("three", map.get("3"));
    }

    @Test
    public void whenThereAreNoDuplicates_allItemsAreAdded() {
        Map<String, String> provided = new HashMap<String, String>();
        provided.put("1", "one");
        provided.put("2", "two");
        provided.put("3", "three");

        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put("4", "four");
        map.put("5", "five");

        map.putAll(provided);

        assertEquals(5, map.size());
        assertEquals("one", map.get("1"));
        assertEquals("two", map.get("2"));
        assertEquals("three", map.get("3"));
        assertEquals("four", map.get("4"));
        assertEquals("five", map.get("5"));
    }

    @Test
    public void whenThereAreSomeDuplicates_nonDuplicatesOnlyAreAdded() {
        Map<String, String> provided = new HashMap<String, String>();
        provided.put("1", "one");
        provided.put("2", "two");
        provided.put("3", "three");

        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put("3", "three");
        map.put("4", "four");

        map.putAll(provided);

        assertEquals(4, map.size());
        assertEquals("one", map.get("1"));
        assertEquals("two", map.get("2"));
        assertEquals("three", map.get("3"));
        assertEquals("four", map.get("4"));
    }

    @Test
    public void whenAllDuplicates_nothingIsAdded() {
        Map<String, String> provided = new HashMap<String, String>();
        provided.put("1", "one");
        provided.put("2", "two");
        provided.put("3", "three");

        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.put("1", "one");
        map.put("2", "two");
        map.put("3", "three");

        map.putAll(provided);

        assertEquals(3, map.size());
        assertEquals("one", map.get("1"));
        assertEquals("two", map.get("2"));
        assertEquals("three", map.get("3"));
    }

    @Test
    @Ignore
    public void whenOneOfTheKeysContainsNull() {

    }
}

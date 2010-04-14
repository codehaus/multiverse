package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMapExcluding;
import static org.multiverse.transactional.collections.CollectionTestUtils.createTreeMapIncluding;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_getTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }


    @Test
    public void whenKeyNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();

        try {
            map.get(null);
            fail();
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void whenTreeEmpty_thenNothingFound() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();

        String result = map.get("foo");
        assertNull(result);
    }

    @Test
    public void whenKeyDoesNotExist() {
        TransactionalTreeMap<String, String> map = createTreeMapExcluding(100, "1");

        String value = map.get("1");
        assertNull(value);
    }

    @Test
    public void whenKeyDoesExist() {
        TransactionalTreeMap<String, String> map = createTreeMapIncluding(100, "1", "one");

        String value = map.get("1");
        assertEquals("one", value);
    }

    @Test
    @Ignore
    public void whenKeyIsRoot() {

    }
}

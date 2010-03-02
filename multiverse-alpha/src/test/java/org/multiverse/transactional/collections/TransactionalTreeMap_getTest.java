package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_getTest {

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
    @Ignore
    public void whenKeyDoesNotExist() {

    }

    @Test
    @Ignore
    public void whenKeyIsRoot() {

    }

    @Test
    public void whenKeyInLeftBranch() {

    }
}

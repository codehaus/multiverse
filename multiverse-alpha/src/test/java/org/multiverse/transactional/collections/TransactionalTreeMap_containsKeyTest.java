package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_containsKeyTest {

    @Test(expected = NullPointerException.class)
    public void whenKeyNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.containsKey(null);
    }

    @Test
    @Ignore
    public void whenKeyNotFound() {

    }

    @Test
    @Ignore
    public void whenKeyFound() {

    }
}

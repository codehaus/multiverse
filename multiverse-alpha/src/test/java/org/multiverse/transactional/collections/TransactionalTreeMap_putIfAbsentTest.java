package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_putIfAbsentTest {

    @Test(expected = NullPointerException.class)
    public void whenKeyNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.putIfAbsent(null, "foo");
    }

    @Test(expected = NullPointerException.class)
    public void whenValueNull_thenNullPointerException() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        map.putIfAbsent("foo", null);
    }

    @Test
    @Ignore
    public void test() {
    }
}

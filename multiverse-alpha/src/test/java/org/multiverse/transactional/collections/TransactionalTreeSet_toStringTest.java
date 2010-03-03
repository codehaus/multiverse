package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_toStringTest {

    @Test
    public void whenEmpty() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        String result = set.toString();
        assertEquals("[]", result);
    }

    @Test
    @Ignore
    public void test() {
    }
}

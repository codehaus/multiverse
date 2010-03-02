package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_containsTest {

    @Test(expected = NullPointerException.class)
    public void whenNullArgument_thenNullPointerException() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.contains(null);
    }

    @Test
    public void whenSetEmpty() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();

        boolean result = set.contains("foo");
        assertFalse(result);
    }

    @Test
    @Ignore
    public void whenItemNotFound() {

    }

    @Test
    @Ignore
    public void whenItemFound() {

    }


}

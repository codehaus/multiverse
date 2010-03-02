package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_addTest {

    @Test
    public void whenSetEmpty() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();

        String item = "foo";
        boolean result = set.add(item);
        assertTrue(result);
        assertTrue(set.contains("foo"));
        assertEquals(1, set.size());
    }

    @Ignore
    @Test
    public void whenItemNotAddedBefore() {

    }

    @Ignore
    @Test
    public void whenItemAddedBefore() {

    }
}

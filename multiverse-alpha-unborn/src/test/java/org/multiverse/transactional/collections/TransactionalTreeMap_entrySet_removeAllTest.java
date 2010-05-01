package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_entrySet_removeAllTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = NullPointerException.class)
    public void whenCollectionNull_thenNullPointerException() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set<Map.Entry> entries = map.entrySet();
        entries.removeAll(null);
    }

    @Test
    public void whenCollectionEmpty_thenCallIgnored() {
        TransactionalTreeMap map = new TransactionalTreeMap();

        Set<Map.Entry> entries = map.entrySet();

        boolean changed = entries.removeAll(new LinkedList());
        assertFalse(changed);
    }

    @Test
    @Ignore
    public void test() {
    }
}

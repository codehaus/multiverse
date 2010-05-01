package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_entrySet_addAllTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void whenCalled_thenUnsupportedOperationException() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set<Map.Entry> entries = map.entrySet();
        entries.addAll(new LinkedList());
    }
}

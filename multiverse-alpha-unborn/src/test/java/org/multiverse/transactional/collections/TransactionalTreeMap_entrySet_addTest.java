package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_entrySet_addTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test(expected = UnsupportedOperationException.class)
    public void whenAdded_thenUnsupportedOperationException() {
        TransactionalTreeMap map = new TransactionalTreeMap();
        Set<Map.Entry> entries = map.entrySet();
        entries.add(mock(Map.Entry.class));
    }
}

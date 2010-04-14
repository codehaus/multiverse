package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_toStringTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

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

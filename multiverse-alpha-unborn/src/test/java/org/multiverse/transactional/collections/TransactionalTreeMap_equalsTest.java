package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_equalsTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }


    @Test
    @Ignore
    public void test() {
    }


    @Test
    public void whenComparedToSelf_thenTrue() {
        TransactionalTreeMap<String, String> map = new TransactionalTreeMap<String, String>();
        assertTrue(map.equals(map));
    }
}


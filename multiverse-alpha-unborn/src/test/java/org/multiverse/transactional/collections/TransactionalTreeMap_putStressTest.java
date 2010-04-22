package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_putStressTest {


    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }


    @Test
    public void bigtest() {
        TransactionalTreeMap<Integer, Integer> map = new TransactionalTreeMap<Integer, Integer>();

        int itemCount = 1000;

        //todo: more diversity in the map.
        for (int k = 0; k < itemCount; k++) {
            map.put(k, k);
        }

        for (int k = 0; k < itemCount; k++) {
            assertTrue(map.containsKey(k));
        }

    }
}

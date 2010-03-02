package org.multiverse.transactional.collections;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_ConstructorTest {

    @Test
    public void test() {
        TransactionalTreeMap treeMap = new TransactionalTreeMap();
        assertEquals(0, treeMap.size());
        assertTrue(treeMap.isEmpty());
    }
}

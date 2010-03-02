package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeMap_equalsTest {

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


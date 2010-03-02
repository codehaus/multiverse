package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_equalsTest {

    @Test
    @Ignore
    public void test() {
    }

    @Test
    public void whenComparedToSelf_thenTrue() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        assertTrue(set.equals(set));
    }
}

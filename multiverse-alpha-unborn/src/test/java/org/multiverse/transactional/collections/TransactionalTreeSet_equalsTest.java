package org.multiverse.transactional.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.multiverse.api.ThreadLocalTransaction.clearThreadLocalTransaction;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_equalsTest {

    @Before
    public void setUp() {
        clearThreadLocalTransaction();
    }

    @Test
    @Ignore
    public void test() {
    }

    @Test
    @Ignore
    public void whenEqualContent() {
    }

    @Test
    public void whenComparedToNull_thenFalse() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        assertFalse(set.equals(null));
    }

    @Test
    public void whenComparedToNonSet_thenFalse() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>("a", "b");
        assertFalse(set.equals("bar"));
        assertFalse(set.equals(Arrays.asList("a", "b")));
    }

    @Test
    public void whenComparedToSelf_thenTrue() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        assertTrue(set.equals(set));
    }
}

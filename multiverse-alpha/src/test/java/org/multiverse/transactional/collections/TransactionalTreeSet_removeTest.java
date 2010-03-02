package org.multiverse.transactional.collections;

import org.junit.Ignore;
import org.junit.Test;

/**
 * @author Peter Veentjer
 */
public class TransactionalTreeSet_removeTest {

    @Test(expected = NullPointerException.class)
    public void whenItemNull_thenNullPointerException() {
        TransactionalTreeSet<String> set = new TransactionalTreeSet<String>();
        set.remove(null);
    }

    @Test
    @Ignore
    public void whenSetEmpty_thenReturnFalse() {

    }

    @Test
    @Ignore
    public void whenItemNotFound_thenNoChange() {

    }

    @Test
    @Ignore
    public void whenItemFound_thenRemoved() {

    }
}
